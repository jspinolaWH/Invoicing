package com.example.invoicing.finvoice;

import com.example.invoicing.entity.customer.BillingProfile;
import com.example.invoicing.invoice.AccountingLedgerEntry;
import com.example.invoicing.invoice.Invoice;
import com.example.invoicing.invoice.InvoiceAttachment;
import com.example.invoicing.invoice.InvoiceLineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinvoiceBuilderService {

    private final SellerDetails seller;

    private static final DateTimeFormatter FINVOICE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Build a FINVOICE 3.0 XML string from an invoice.
     * Handles multi-account ledger splits, mixed VAT rates, and reverse charge.
     */
    public String build(Invoice invoice) {
        if (invoice.isReverseChargeVat()) {
            validateReverseChargePrerequisites(invoice);
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element root = doc.createElement("Finvoice");
            root.setAttribute("Version", "3.0");
            doc.appendChild(root);

            appendMessageTransmissionDetails(doc, root, invoice);
            appendSellerPartyDetails(doc, root);
            appendBuyerPartyDetails(doc, root, invoice);
            appendInvoiceDetails(doc, root, invoice);
            appendInvoiceRows(doc, root, invoice);
            appendVatSpecificationDetails(doc, root, invoice);
            appendInvoiceAmountDetails(doc, root, invoice);

            if (invoice.getCustomText() != null && !invoice.getCustomText().isBlank()) {
                Element freeText = doc.createElement("InvoiceFreeText");
                freeText.setTextContent(invoice.getCustomText());
                root.appendChild(freeText);
            }

            if (invoice.isReverseChargeVat()) {
                Element rcText = doc.createElement("InvoiceFreeText");
                rcText.setTextContent("Käännetty verovelvollisuus / Omvänd skattskyldighet (AVL 8c §)");
                root.appendChild(rcText);
            }

            appendAttachmentDetails(doc, root, invoice);

            return serializeDocument(doc);
        } catch (FinvoiceValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new FinvoiceValidationException("FINVOICE XML generation failed: " + e.getMessage());
        }
    }

    private void appendMessageTransmissionDetails(Document doc, Element root, Invoice invoice) {
        Element mtd = doc.createElement("MessageTransmissionDetails");
        addChild(doc, mtd, "MessageSenderId", seller.getVatNumber());
        addChild(doc, mtd, "MessageReceiverId",
            invoice.getCustomer().getBillingProfile() != null
                ? invoice.getCustomer().getBillingProfile().getCustomerIdNumber() : "");
        addChild(doc, mtd, "MessageIdentifier",
            invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "DRAFT");
        if (invoice.getInvoiceDate() != null) {
            addChild(doc, mtd, "MessageTimeStamp", invoice.getInvoiceDate().format(FINVOICE_DATE));
        }
        root.appendChild(mtd);
    }

    private void appendSellerPartyDetails(Document doc, Element root) {
        Element spd = doc.createElement("SellerPartyDetails");

        Element sellerIdVat = doc.createElement("SellerPartyIdentifier");
        sellerIdVat.setAttribute("IdentifierType", "VATNumber");
        sellerIdVat.setTextContent(seller.getVatNumber());
        spd.appendChild(sellerIdVat);

        addChild(doc, spd, "SellerOrganisationName", seller.getOrganisationName());
        addChild(doc, spd, "SellerStreetName", seller.getStreetName());
        addChild(doc, spd, "SellerTownName", seller.getTownName());
        addChild(doc, spd, "SellerPostCodeIdentifier", seller.getPostCode());
        addChild(doc, spd, "CountryCode", seller.getCountryCode());

        if (seller.getBankAccountIban() != null) {
            Element bank = doc.createElement("SellerAccountDetails");
            addChild(doc, bank, "SellerAccountID", seller.getBankAccountIban());
            if (seller.getBicCode() != null) {
                addChild(doc, bank, "SellerBic", seller.getBicCode());
            }
            spd.appendChild(bank);
        }
        root.appendChild(spd);
    }

    private void appendBuyerPartyDetails(Document doc, Element root, Invoice invoice) {
        Element bpd = doc.createElement("BuyerPartyDetails");

        BillingProfile profile = invoice.getCustomer().getBillingProfile();

        Element buyerIdNum = doc.createElement("BuyerPartyIdentifier");
        buyerIdNum.setAttribute("IdentifierType", "CustomerNumber");
        buyerIdNum.setTextContent(profile != null ? profile.getCustomerIdNumber() : "");
        bpd.appendChild(buyerIdNum);

        if (invoice.isReverseChargeVat() && profile != null && profile.getBusinessId() != null) {
            Element buyerIdVat = doc.createElement("BuyerPartyIdentifier");
            buyerIdVat.setAttribute("IdentifierType", "VATNumber");
            buyerIdVat.setTextContent("FI" + profile.getBusinessId().replaceAll("[^0-9]", ""));
            bpd.appendChild(buyerIdVat);
        }

        addChild(doc, bpd, "BuyerOrganisationName", invoice.getCustomer().getName());

        if (profile != null && profile.getBillingAddress() != null) {
            addChild(doc, bpd, "BuyerStreetName", profile.getBillingAddress().getStreetAddress());
            addChild(doc, bpd, "BuyerTownName", profile.getBillingAddress().getCity());
            addChild(doc, bpd, "BuyerPostCodeIdentifier", profile.getBillingAddress().getPostalCode());
        }
        addChild(doc, bpd, "CountryCode", "FI");

        root.appendChild(bpd);
    }

    private void appendInvoiceDetails(Document doc, Element root, Invoice invoice) {
        Element id = doc.createElement("InvoiceDetails");

        addChild(doc, id, "InvoiceTypeText", invoice.getInvoiceType() != null
            && invoice.getInvoiceType().name().equals("CREDIT_NOTE") ? "CREDIT NOTE" : "INVOICE");

        Element typeCode = doc.createElement("InvoiceTypeCode");
        typeCode.setTextContent(invoice.getInvoiceType() != null
            && invoice.getInvoiceType().name().equals("CREDIT_NOTE") ? "CRE" : "INV01");
        id.appendChild(typeCode);

        if (invoice.getTemplateCode() != null) {
            Element tmplCode = doc.createElement("InvoiceTypeCode");
            tmplCode.setAttribute("IdentifierType", "TemplateCode");
            tmplCode.setTextContent(invoice.getTemplateCode());
            id.appendChild(tmplCode);
        }

        addChild(doc, id, "InvoiceNumber",
            invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");

        if (invoice.getInvoiceDate() != null) {
            Element invDate = doc.createElement("InvoiceDate");
            invDate.setAttribute("Format", "CCYYMMDD");
            invDate.setTextContent(invoice.getInvoiceDate().format(FINVOICE_DATE));
            id.appendChild(invDate);
        }
        if (invoice.getDueDate() != null) {
            Element dueDate = doc.createElement("InvoiceDueDate");
            dueDate.setAttribute("Format", "CCYYMMDD");
            dueDate.setTextContent(invoice.getDueDate().format(FINVOICE_DATE));
            id.appendChild(dueDate);
        }

        root.appendChild(id);
    }

    private void appendInvoiceRows(Document doc, Element root, Invoice invoice) {
        int rowIndex = 1;
        for (InvoiceLineItem line : invoice.getLineItems()) {
            if (!line.getLedgerEntries().isEmpty()) {
                for (AccountingLedgerEntry entry : line.getLedgerEntries()) {
                    root.appendChild(buildInvoiceRowElement(doc, line, entry, rowIndex++));
                }
            } else {
                root.appendChild(buildInvoiceRowSingleElement(doc, line, rowIndex++));
            }
        }
    }

    Element buildInvoiceRowElement(Document doc, InvoiceLineItem line,
                                             AccountingLedgerEntry entry, int rowIndex) {
        Element row = doc.createElement("InvoiceRow");

        if (line.getProduct() != null) {
            addChild(doc, row, "ArticleIdentifier", line.getProduct().getCode());
        }
        addChild(doc, row, "ArticleName", line.getDescription());

        Element qty = doc.createElement("DeliveredQuantity");
        qty.setAttribute("QuantityUnitCode", "pcs");
        qty.setTextContent(line.getQuantity().setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(qty);

        Element unitPrice = doc.createElement("UnitPriceAmount");
        unitPrice.setAttribute("AmountCurrencyIdentifier", "EUR");
        unitPrice.setTextContent(line.getUnitPrice().setScale(4, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(unitPrice);

        boolean reverseCharge = invoice(line);
        BigDecimal effectiveVatRate = resolveEffectiveVatRate(line, reverseCharge);
        addChild(doc, row, "RowVatRatePercent",
            effectiveVatRate.setScale(2, RoundingMode.HALF_UP).toPlainString());

        BigDecimal rowNet = entry.getAmount() != null ? entry.getAmount() : line.getNetAmount();
        BigDecimal rowVat = entry.getVatAmount() != null ? entry.getVatAmount() : line.getGrossAmount().subtract(line.getNetAmount());
        BigDecimal rowGross = rowNet.add(rowVat);

        Element vatAmt = doc.createElement("RowVatAmount");
        vatAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
        vatAmt.setTextContent(rowVat.setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(vatAmt);

        Element netAmt = doc.createElement("RowAmountWithoutVat");
        netAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
        netAmt.setTextContent(rowNet.setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(netAmt);

        Element grossAmt = doc.createElement("RowAmountWithVat");
        grossAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
        grossAmt.setTextContent(rowGross.setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(grossAmt);

        Element rowId = doc.createElement("RowIdentifier");
        rowId.setTextContent(String.valueOf(rowIndex));
        row.appendChild(rowId);

        if (entry.getLedgerCode() != null) {
            Element ledgerCode = doc.createElement("RowIdentifier");
            ledgerCode.setAttribute("IdentifierType", "LedgerCode");
            ledgerCode.setTextContent(entry.getLedgerCode());
            row.appendChild(ledgerCode);
        }

        return row;
    }

    private Element buildInvoiceRowSingleElement(Document doc, InvoiceLineItem line, int rowIndex) {
        Element row = doc.createElement("InvoiceRow");

        if (line.getProduct() != null) {
            addChild(doc, row, "ArticleIdentifier", line.getProduct().getCode());
        }
        addChild(doc, row, "ArticleName", line.getDescription());

        Element qty = doc.createElement("DeliveredQuantity");
        qty.setAttribute("QuantityUnitCode", "pcs");
        qty.setTextContent(line.getQuantity().setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(qty);

        Element unitPrice = doc.createElement("UnitPriceAmount");
        unitPrice.setAttribute("AmountCurrencyIdentifier", "EUR");
        unitPrice.setTextContent(line.getUnitPrice().setScale(4, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(unitPrice);

        BigDecimal effectiveVatRate = resolveEffectiveVatRate(line, invoice(line));
        addChild(doc, row, "RowVatRatePercent",
            effectiveVatRate.setScale(2, RoundingMode.HALF_UP).toPlainString());

        BigDecimal vatAmt = line.getGrossAmount().subtract(line.getNetAmount());

        Element vatAmtEl = doc.createElement("RowVatAmount");
        vatAmtEl.setAttribute("AmountCurrencyIdentifier", "EUR");
        vatAmtEl.setTextContent(vatAmt.setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(vatAmtEl);

        Element netAmt = doc.createElement("RowAmountWithoutVat");
        netAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
        netAmt.setTextContent(line.getNetAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(netAmt);

        Element grossAmt = doc.createElement("RowAmountWithVat");
        grossAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
        grossAmt.setTextContent(line.getGrossAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        row.appendChild(grossAmt);

        Element rowId = doc.createElement("RowIdentifier");
        rowId.setTextContent(String.valueOf(rowIndex));
        row.appendChild(rowId);

        if (line.getAccountingAccount() != null) {
            Element ledgerCode = doc.createElement("RowIdentifier");
            ledgerCode.setAttribute("IdentifierType", "LedgerCode");
            ledgerCode.setTextContent(line.getAccountingAccount().getCode());
            row.appendChild(ledgerCode);
        }

        if (invoice(line) && line.getVatRate().compareTo(BigDecimal.ZERO) == 0) {
            addChild(doc, row, "VatCode", "RC");
        }

        return row;
    }

    private void appendVatSpecificationDetails(Document doc, Element root, Invoice invoice) {
        Map<BigDecimal, VatGroup> vatGroups = groupLinesByVatRate(invoice.getLineItems(),
            invoice.isReverseChargeVat());

        BigDecimal totalBaseFromGroups = BigDecimal.ZERO;
        BigDecimal totalBase = invoice.getNetAmount() != null ? invoice.getNetAmount() : BigDecimal.ZERO;

        List<Map.Entry<BigDecimal, VatGroup>> entries = new ArrayList<>(vatGroups.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            VatGroup group = entries.get(i).getValue();

            // Last group absorbs rounding difference
            BigDecimal baseAmount;
            if (i == entries.size() - 1) {
                baseAmount = totalBase.subtract(totalBaseFromGroups).setScale(2, RoundingMode.HALF_UP);
            } else {
                baseAmount = group.getBaseAmount().setScale(2, RoundingMode.HALF_UP);
                totalBaseFromGroups = totalBaseFromGroups.add(baseAmount);
            }

            BigDecimal vatAmount = baseAmount
                .multiply(group.getVatRate().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);

            Element vsd = doc.createElement("VatSpecificationDetails");

            Element baseAmt = doc.createElement("VatBaseAmount");
            baseAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
            baseAmt.setTextContent(baseAmount.toPlainString());
            vsd.appendChild(baseAmt);

            addChild(doc, vsd, "VatRatePercent",
                group.getVatRate().setScale(2, RoundingMode.HALF_UP).toPlainString());

            Element vatAmt = doc.createElement("VatRateAmount");
            vatAmt.setAttribute("AmountCurrencyIdentifier", "EUR");
            vatAmt.setTextContent(vatAmount.toPlainString());
            vsd.appendChild(vatAmt);

            root.appendChild(vsd);
        }
    }

    private void appendInvoiceAmountDetails(Document doc, Element root, Invoice invoice) {
        Element iad = doc.createElement("InvoiceAmountDetails");

        BigDecimal net = invoice.getNetAmount() != null ? invoice.getNetAmount() : BigDecimal.ZERO;
        BigDecimal gross = invoice.getGrossAmount() != null ? invoice.getGrossAmount() : BigDecimal.ZERO;
        BigDecimal vat = invoice.getVatAmount() != null ? invoice.getVatAmount() : gross.subtract(net);

        Element netEl = doc.createElement("InvoiceTotalVatExcludedAmount");
        netEl.setAttribute("AmountCurrencyIdentifier", "EUR");
        netEl.setTextContent(net.setScale(2, RoundingMode.HALF_UP).toPlainString());
        iad.appendChild(netEl);

        Element vatEl = doc.createElement("InvoiceTotalVatAmount");
        vatEl.setAttribute("AmountCurrencyIdentifier", "EUR");
        vatEl.setTextContent(vat.setScale(2, RoundingMode.HALF_UP).toPlainString());
        iad.appendChild(vatEl);

        Element grossEl = doc.createElement("InvoiceTotalVatIncludedAmount");
        grossEl.setAttribute("AmountCurrencyIdentifier", "EUR");
        grossEl.setTextContent(gross.setScale(2, RoundingMode.HALF_UP).toPlainString());
        iad.appendChild(grossEl);

        root.appendChild(iad);
    }

    private void appendAttachmentDetails(Document doc, Element root, Invoice invoice) {
        for (InvoiceAttachment att : invoice.getAttachments()) {
            Element ad = doc.createElement("AttachmentDetails");
            addChild(doc, ad, "AttachmentIdentifier", att.getAttachmentIdentifier());
            addChild(doc, ad, "AttachmentName", att.getFilename());
            addChild(doc, ad, "AttachmentMimeType", att.getMimeType());
            if (att.getSecurityClass() != null) {
                addChild(doc, ad, "AttachmentSecurityClass", att.getSecurityClass());
            }
            if (att.getContentBase64() != null) {
                addChild(doc, ad, "AttachmentContent", att.getContentBase64());
            }
            root.appendChild(ad);
        }
    }

    Map<BigDecimal, VatGroup> groupLinesByVatRate(List<InvoiceLineItem> lines, boolean reverseCharge) {
        Map<BigDecimal, VatGroup> groups = new LinkedHashMap<>();
        for (InvoiceLineItem line : lines) {
            BigDecimal rate = resolveEffectiveVatRate(line, reverseCharge);
            // Use compareTo for scale-insensitive grouping
            BigDecimal key = groups.keySet().stream()
                .filter(k -> k.compareTo(rate) == 0)
                .findFirst()
                .orElse(rate);
            VatGroup group = groups.computeIfAbsent(key, r -> new VatGroup(r, BigDecimal.ZERO, BigDecimal.ZERO));
            group.setBaseAmount(group.getBaseAmount().add(line.getNetAmount()));
            BigDecimal vatAmt = line.getGrossAmount().subtract(line.getNetAmount());
            group.setVatAmount(group.getVatAmount().add(vatAmt));
        }
        return groups;
    }

    void validateReverseChargePrerequisites(Invoice invoice) {
        BillingProfile profile = invoice.getCustomer() != null
            ? invoice.getCustomer().getBillingProfile() : null;
        if (profile == null || profile.getBusinessId() == null || profile.getBusinessId().isBlank()) {
            throw new FinvoiceValidationException("Reverse charge requires buyer VAT number (businessId)");
        }
    }

    private BigDecimal resolveEffectiveVatRate(InvoiceLineItem line, boolean reverseCharge) {
        if (reverseCharge && line.getProduct() != null && line.getProduct().isReverseChargeVat()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return line.getVatRate() != null ? line.getVatRate() : BigDecimal.ZERO.setScale(2);
    }

    // Overload used in buildInvoiceRowSingleElement to check invoice-level reverseCharge
    private boolean invoice(InvoiceLineItem line) {
        return line.getInvoice() != null && line.getInvoice().isReverseChargeVat();
    }

    private String serializeDocument(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    private void addChild(Document doc, Element parent, String tag, String text) {
        if (text == null) return;
        Element el = doc.createElement(tag);
        el.setTextContent(text);
        parent.appendChild(el);
    }
}
