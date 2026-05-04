#!/usr/bin/env python3
"""Generate Ralph skill activity presentation — plain-language edition."""

import os, io
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from PIL import Image, ImageDraw, ImageFont

# ── Slide dimensions ─────────────────────────────────────────────────────────
SW, SH = Inches(13.33), Inches(7.5)

# ── Colours ──────────────────────────────────────────────────────────────────
BG  = (15, 17, 23);   CARD = (22, 27, 39);   TERM = (13, 17, 23)
HDR = (24, 31, 46);   BDR  = (45, 55, 72);   TXT  = (226, 232, 240)
DIM = (100, 116, 139);GRN  = (52, 211, 153);  G2   = (6, 150, 105)
PUR = (167, 139, 250);ORG  = (251, 146, 60);  CYN  = (125, 211, 252)
RED = (248, 113, 113);YLW  = (252, 211, 77);  WHT  = (248, 250, 252)
GRY = (71, 85, 105);  NAVY = (30, 41, 59)

# ── Font loaders ─────────────────────────────────────────────────────────────
def _ttf(paths, sz):
    for p in paths:
        if os.path.exists(p):
            try: return ImageFont.truetype(p, sz)
            except: pass
    return ImageFont.load_default()

def mono(sz):   return _ttf(['/mnt/c/Windows/Fonts/consola.ttf',
                              '/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf'], sz)
def monob(sz):  return _ttf(['/mnt/c/Windows/Fonts/consolab.ttf',
                              '/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf'], sz)
def sans(sz):   return _ttf(['/mnt/c/Windows/Fonts/segoeui.ttf',
                              '/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf',
                              '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'], sz)
def sansb(sz):  return _ttf(['/mnt/c/Windows/Fonts/segoeuib.ttf',
                              '/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf',
                              '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'], sz)

def tw(font, text):
    try: return int(font.getlength(text))
    except:
        try: b = font.getbbox(text); return b[2] - b[0]
        except: return len(text) * 11

# ── Image helpers ─────────────────────────────────────────────────────────────
def stream(img):
    buf = io.BytesIO(); img.save(buf, 'PNG'); buf.seek(0); return buf

# ── pptx helpers ─────────────────────────────────────────────────────────────
def set_bg(slide, color):
    f = slide.background.fill; f.solid(); f.fore_color.rgb = RGBColor(*color)

def txt(slide, text, l, t, w, h, sz=18, color=TXT, bold=False,
        align=PP_ALIGN.LEFT, name='Segoe UI', wrap=True):
    tb = slide.shapes.add_textbox(l, t, w, h)
    tf = tb.text_frame; tf.word_wrap = wrap
    p = tf.paragraphs[0]; p.alignment = align
    r = p.add_run(); r.text = text
    r.font.size = Pt(sz); r.font.color.rgb = RGBColor(*color)
    r.font.bold = bold; r.font.name = name
    return tb

def box(slide, l, t, w, h, fill, outline=None, lw=1):
    s = slide.shapes.add_shape(1, l, t, w, h)
    s.fill.solid(); s.fill.fore_color.rgb = RGBColor(*fill)
    if outline: s.line.color.rgb = RGBColor(*outline); s.line.width = Pt(lw)
    else: s.line.fill.background()
    return s

def img(slide, image, l, t, w=None, h=None):
    kw = {}
    if w: kw['width'] = w
    if h: kw['height'] = h
    return slide.shapes.add_picture(stream(image), l, t, **kw)

def note(slide, text):
    slide.notes_slide.notes_text_frame.text = text

# ── Terminal renderer ─────────────────────────────────────────────────────────
class T:
    def __init__(self, w=1580, fs=21, lh=34, pad=32, title="ralph — automated build checker"):
        self.w = w; self.lh = lh; self.pad = pad; self.title = title
        self.f = mono(fs); self.ft = sans(14); self.lines = []

    def _a(self, segs): self.lines.append(segs); return self

    def prompt(self, cmd):     return self._a([('> ', GRN), (cmd, WHT)])
    def o(self, t, c=TXT):    return self._a([(t, c)])
    def dim(self, t):          return self._a([(t, DIM)])
    def ok(self, t):           return self._a([(t, GRN)])
    def err(self, t):          return self._a([(t, RED)])
    def warn(self, t):         return self._a([(t, YLW)])
    def info(self, t):         return self._a([(t, CYN)])
    def blank(self, n=1):
        for _ in range(n): self.lines.append([])
        return self
    def sep(self, n=52):       return self._a([('─' * n, GRY)])
    def multi(self, *segs):    return self._a(list(segs))

    def render(self):
        HH = 48
        h = HH + self.pad * 2 + len(self.lines) * self.lh + 4
        im = Image.new('RGB', (self.w, h), TERM)
        d = ImageDraw.Draw(im)
        d.rectangle([0, 0, self.w, HH], fill=HDR)
        cy = HH // 2
        for x, c in [(22,(255,95,86)),(48,(255,189,46)),(74,(39,201,63))]:
            d.ellipse([x-9,cy-9,x+9,cy+9], fill=c)
        d.text((self.w//2, cy), self.title, fill=DIM, font=self.ft, anchor='mm')
        d.line([0, HH, self.w, HH], fill=BDR, width=1)
        d.rectangle([0,0,self.w-1,h-1], outline=BDR, width=1)
        y = HH + self.pad
        for segs in self.lines:
            x = self.pad
            for text, color in segs:
                d.text((x, y), text, fill=color, font=self.f)
                x += tw(self.f, text)
            y += self.lh
        return im


# ── Reusable tag label ────────────────────────────────────────────────────────
def tag(slide, label, color, left=Inches(0.5), top=Inches(0.35)):
    b = slide.shapes.add_shape(1, left, top, Inches(3.4), Inches(0.38))
    b.fill.solid(); b.fill.fore_color.rgb = RGBColor(*NAVY)
    b.line.color.rgb = RGBColor(*color); b.line.width = Pt(1)
    txt(slide, label, left + Inches(0.1), top + Inches(0.04),
        Inches(3.2), Inches(0.32), sz=12, color=color, bold=True,
        name='Segoe UI', wrap=False)


# ═══════════════════════════════════════════════════════════════════════════
#  SLIDE BUILDERS
# ═══════════════════════════════════════════════════════════════════════════

def s_title(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    box(s, 0, 0, SW, Inches(0.06), GRN)
    txt(s, 'RALPH', Inches(0.8), Inches(1.5), Inches(11.7), Inches(2.5),
        sz=110, color=GRN, bold=True, align=PP_ALIGN.CENTER, name='Consolas', wrap=False)
    txt(s, '& ralph-brother', Inches(0.8), Inches(3.65), Inches(11.7), Inches(0.85),
        sz=38, color=ORG, align=PP_ALIGN.CENTER, wrap=False)
    txt(s, 'An AI that builds and verifies your product — automatically',
        Inches(0.8), Inches(4.65), Inches(11.7), Inches(0.6),
        sz=21, color=DIM, align=PP_ALIGN.CENTER, wrap=False)
    box(s, 0, SH - Inches(0.06), SW, Inches(0.06), GRN)
    note(s, "Welcome.\n\nThis presentation explains Ralph and Ralph-brother — two AI tools that automatically check whether features have been built correctly and are visible to users.\n\nNo technical knowledge required. Think of them like a very fast, very thorough quality checker that never gets tired.")
    return s


def s_agenda(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    box(s, 0, 0, SW, Inches(0.06), GRN)
    txt(s, "What we're covering today", Inches(0.8), Inches(0.3),
        Inches(11), Inches(0.7), sz=30, color=WHT, bold=True)
    items = [
        ('01', 'The Problem',        'Checking 52 features by hand takes days — and you miss things',   RED),
        ('02', 'Ralph',              'An AI that checks the code and fixes whatever is missing',         GRN),
        ('03', 'Ralph-Brother',      'Same as Ralph, but also checks if users can actually see it',      ORG),
        ('04', 'UI-Loop',            'Scans the app first so Ralph-Brother knows where to look',         PUR),
        ('05', 'All Together',       'The full pipeline — how they connect and what they produce',       CYN),
        ('06', 'Discovery',          '52 requirements → functional prototype: tools, setup, thoughts',   CYN),
        ('07', 'Planning P&P',       'Grooming, task breakdown, API contracts, and estimation',          YLW),
    ]
    y = Inches(1.2)
    for num, title, desc, color in items:
        b = slide_shapes_add_shape_helper(s, Inches(0.7), y, Inches(0.55), Inches(0.46), NAVY, color)
        txt(s, num, Inches(0.73), y + Inches(0.06), Inches(0.46), Inches(0.36),
            sz=17, color=color, bold=True, name='Consolas', wrap=False, align=PP_ALIGN.CENTER)
        txt(s, title, Inches(1.5), y + Inches(0.03), Inches(4.5), Inches(0.42),
            sz=21, color=WHT, bold=True, wrap=False)
        txt(s, desc, Inches(1.5), y + Inches(0.44), Inches(11), Inches(0.34),
            sz=13, color=DIM, wrap=False)
        y += Inches(0.9)
    note(s, "Agenda.\n\n01 — The Problem: We'll see what it looks like to check features manually and why it breaks down at scale.\n02 — Ralph: The first AI loop. It reads the task list and fixes whatever code is missing.\n03 — Ralph-Brother: A stricter version that also checks whether users can actually find and click each feature.\n04 — UI-Loop: Runs before Ralph-Brother to scan what's already visible on screen.\n05 — All Together: How you run them in sequence to go from nothing to a fully verified product.\n06 — Discovery: How we went from 52 requirements to a functional prototype — tools, structured docs, build order.\n07 — Planning P&P: How we planned the next feature with the full team — grooming, API contracts, estimation.")
    return s

def slide_shapes_add_shape_helper(slide, l, t, w, h, fill, outline):
    s = slide.shapes.add_shape(1, l, t, w, h)
    s.fill.solid(); s.fill.fore_color.rgb = RGBColor(*fill)
    s.line.color.rgb = RGBColor(*outline); s.line.width = Pt(1)
    return s

def s_section(prs, layout, num, label, color, sub=''):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    box(s, 0, 0, Inches(0.14), SH, color)
    txt(s, f'{num:02d}', Inches(0.5), Inches(0.3), Inches(4.5), Inches(5.5),
        sz=300, color=NAVY, bold=True, name='Consolas', wrap=False)
    txt(s, label, Inches(5.0), Inches(2.1), Inches(8.1), Inches(1.6),
        sz=66, color=color, bold=True, wrap=False)
    if sub:
        txt(s, sub, Inches(5.0), Inches(3.85), Inches(7.8), Inches(1.2),
            sz=23, color=DIM, wrap=True)
    return s


# ── 01: The Problem ───────────────────────────────────────────────────────────
def s_problem(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '01 — The Problem', RED)

    t = T(title="a developer checking features manually")
    t.o("Checking task 1 of 52:", WHT)
    t.o('  "Users can filter their invoice list by status"', CYN)
    t.blank()
    t.dim("  Opening files...  reading code...  cross-referencing the spec...")
    t.dim("  [20 minutes of reading later]")
    t.dim("  There's something here... or is this a different feature?")
    t.dim("  Need to also check if the button appears on screen...")
    t.dim("  [opens the app, clicks around for 10 minutes]")
    t.dim("  Can't find it. Maybe it's on another page. Moving on.")
    t.blank()
    t.sep()
    t.multi(('  Time spent on task 1:      ', DIM), ('~45 minutes', YLW))
    t.multi(('  52 tasks total:            ', DIM), ('~39 hours of checking', RED))
    t.multi(('  Features definitely missed:', DIM), ('Unknown. Almost certainly some.', RED))
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "The Problem.\n\nBefore Ralph, checking whether 52 features were correctly built meant a developer had to:\n- Open each feature in the task list\n- Hunt through dozens of code files to see if it was implemented\n- Then open the actual app and click around to see if it was visible\n\nThis process is:\n- SLOW: roughly 45 minutes per feature × 52 features = about 39 hours (nearly a full work week)\n- UNRELIABLE: it's very easy to misread a requirement or miss a connected file\n- INCONSISTENT: two developers would often reach different conclusions about the same feature\n- TEDIOUS: nobody does their best thinking after manually reading their 20th file of the day\n\nRalph automates this entirely.")
    return s


# ── 02: Ralph ────────────────────────────────────────────────────────────────
def s_ralph_start(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '02 — Ralph  ›  Starting up', GRN)

    t = T(title="ralph — starting up")
    t.prompt("start ralph")
    t.blank()
    t.ok("  Ralph is ready.")
    t.blank()
    t.o("  Found your task list.  52 features to verify.", WHT)
    t.dim("  I'll check each one automatically — no human needed.")
    t.blank()
    t.o("  Here is how I work:", DIM)
    t.multi(('    1.  ', GRN), ('Read the next task from the list', WHT))
    t.multi(('    2.  ', GRN), ('Check if it has been built',       WHT))
    t.multi(('    3.  ', GRN), ('If something is missing → build it', WHT))
    t.multi(('    4.  ', GRN), ('Check again to confirm the fix worked', WHT))
    t.multi(('    5.  ', GRN), ('Move on to the next task',         WHT))
    t.blank()
    t.sep()
    t.o("  Starting task 1 of 52...", DIM)
    t.o('  "The e-invoice system recognises order types"', CYN)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph starting up.\n\nWhen you run Ralph you give it a task list (a file called prd.json that lists all 52 features that need to exist). Ralph reads it and works through each feature one by one.\n\nThe loop is simple: Check → Fix → Check again → move on.\n\nIf it can't fix something after 3 attempts it flags it for a human developer to look at.\n\nNo developer has to sit there and watch it. It runs on its own.")
    return s


def s_ralph_gapcheck(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '02 — Ralph  ›  Checking the task', GRN)

    t = T(title="ralph — checking task 1")
    t.o('  Checking:  "The e-invoice system recognises order types"', CYN)
    t.blank()
    t.multi(('  Is "start order" logic in the code?          ', TXT), ('Missing   ', RED), ('✗', RED))
    t.multi(('  Is "stop order" logic in the code?           ', TXT), ('Missing   ', RED), ('✗', RED))
    t.multi(('  Does the screen update when an order changes?', TXT), ('Missing   ', RED), ('✗', RED))
    t.blank()
    t.sep()
    t.warn("  Verdict:  3 things are missing.")
    t.o("  Ralph will now build them automatically.", DIM)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph checking a task.\n\nFor each feature, Ralph breaks it down into its individual requirements (the specific things that need to be true for the feature to work).\n\nFor each requirement it asks: is this in the code? It searches through the codebase and gives a simple verdict:\n- Found and complete → tick\n- Found but incomplete → partial\n- Not found at all → missing\n\nIf anything is missing or incomplete, Ralph moves straight to fixing it.")
    return s


def s_ralph_codefix(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '02 — Ralph  ›  Building the missing pieces', GRN)

    t = T(title="ralph — building what's missing")
    t.o("  Building the missing pieces for task 1...", DIM)
    t.blank()
    t.multi(('  Building:   "start order" logic              ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.multi(('  Building:   "stop order" logic               ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.multi(('  Connecting:  screen display to order status   ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.blank()
    t.sep()
    t.ok("  All 3 pieces built!")
    t.o("  Checking again to make sure everything works...", DIM)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph building the missing pieces.\n\nOnce Ralph finds gaps, it doesn't stop and ask a developer to fix them. It fixes them itself.\n\nIt reads the gap report it just wrote and implements each missing piece:\n- Backend logic: the rules and calculations that run on the server\n- Frontend: the buttons, screens, and displays users interact with\n- Connections between the two\n\nAfter building, it always checks again from scratch to confirm the fix actually worked. It doesn't trust itself — it verifies.")
    return s


def s_ralph_done(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '02 — Ralph  ›  Done', GRN)

    t = T(title="ralph — re-checking after fix")
    t.o('  Re-checking task 1 after the fixes...', DIM)
    t.blank()
    t.multi(('  "start order" logic works?         ', TXT), ('Yes   ', GRN), ('✓', GRN))
    t.multi(('  "stop order" logic works?           ', TXT), ('Yes   ', GRN), ('✓', GRN))
    t.multi(('  Screen updates when order changes?  ', TXT), ('Yes   ', GRN), ('✓', GRN))
    t.blank()
    t.ok("  Task 1:  PASSED  ✓   Moving to task 2...")
    t.blank()
    t.dim("  [ ... ralph checks and fixes 48 more tasks ... ]")
    t.blank()
    t.sep()
    t.o("  ╔═══════════════════════════════════════╗", GRN)
    t.o("  ║  Ralph is done!                        ║", GRN)
    t.o("  ║                                        ║", GRN)
    t.multi(('  ║  ', GRN), ('49 tasks:  Everything works      ', WHT), ('✓  ║', GRN))
    t.multi(('  ║  ', GRN), (' 3 tasks:  Need a human to look  ', YLW), ('⚠  ║', GRN))
    t.multi(('  ║  ', GRN), (' 0 tasks:  Still broken          ', DIM), ('✗  ║', GRN))
    t.o("  ╚═══════════════════════════════════════╝", GRN)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph completing.\n\nAfter the fix, Ralph re-checks the same task from scratch. This time all requirements pass. It marks the task complete and moves on.\n\nFinal result:\n- 49 out of 52 tasks: fully working code\n- 3 tasks: flagged for a human developer (these needed database changes or architectural decisions that Ralph can't safely make on its own)\n- 0 tasks still broken: everything was either fixed or escalated\n\nImportant: Ralph only cares about whether the CODE is correct. It doesn't check whether a user sitting at a browser can actually find and use each feature. That's what Ralph-Brother is for.")
    return s


# ── 03: Ralph-Brother ────────────────────────────────────────────────────────
def s_brother_start(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '03 — Ralph-Brother  ›  Starting up', ORG)

    t = T(title="ralph-brother — starting up")
    t.prompt("start ralph-brother")
    t.blank()
    t.ok("  Ralph-Brother is ready.")
    t.blank()
    t.o("  Ralph already checked that the code exists.", WHT)
    t.o("  Ralph-Brother asks a harder question:", WHT)
    t.blank()
    t.multi(('  ', DIM), ('"Can a real user actually FIND and USE each feature?"', ORG))
    t.blank()
    t.o("  Think of it this way:", DIM)
    t.multi(('    Ralph         ', GRN), ('→  checks the kitchen has the ingredients', DIM))
    t.multi(('    Ralph-Brother ', ORG), ('→  checks the food is actually on the menu', DIM))
    t.blank()
    t.sep()
    t.warn("  Found 32 features that users can't fully see yet.")
    t.o("  Starting checks...", DIM)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph-Brother starting up.\n\nThe key difference from Ralph: Ralph-Brother's definition of 'done' is stricter.\n\nRalph says: 'the code that implements this feature exists'\nRalph-Brother says: 'a real user sitting in front of the app can find, click, and use this feature'\n\nA feature can pass Ralph (code is there) but still fail Ralph-Brother (nobody can find the button, the page isn't linked, the dropdown is hidden behind 3 menus).\n\nRalph-Brother's queue is different from Ralph's:\n- It picks up features where the UI is incomplete or missing\n- It also picks up features where Ralph already found the code was broken\n- It skips features where both the code AND the UI are confirmed working\n\nIn this run: 32 features need attention.")
    return s


def s_brother_synthesis(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '03 — Ralph-Brother  ›  The key difference', ORG)

    t = T(title="ralph-brother — what makes it different")
    t.o('  Checking:  "Invoice list can be filtered by status"', CYN)
    t.blank()
    t.o("  Step 1 — Is the code there?", DIM)
    t.multi(('    Filter logic in the code:    ', DIM), ('Partly done   ', YLW), ('⚠', YLW))
    t.blank()
    t.multi(('  Step 2 — Can users actually SEE it?  ', DIM), (' ← Ralph cannot do this part', YLW))
    t.o("    Opening the app...", DIM)
    t.o("    Going to the Invoice List page...", DIM)
    t.o("    Looking for the filter button...", DIM)
    t.err("    ✗  The filter doesn't appear anywhere on screen.")
    t.blank()
    t.o("  The code is there — but nobody can find it.", WHT)
    t.o("  Ralph-Brother will fix BOTH the code AND the screen.", ORG)
    t.o("  That is the difference from plain Ralph.", DIM)

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "The key difference: Step 2 — checking the actual screen.\n\nThis is the step that only Ralph-Brother does. Ralph never looks at the actual app.\n\nIn this example:\n- Step 1 (same as Ralph): the code for a status filter partly exists\n- Step 2 (only Ralph-Brother): opens the app and looks for the filter button... it's not there\n\nThis is a real situation that happens often: a developer builds the backend logic but forgets to wire it up to the screen. Or they add it to the wrong page. Or it's hidden behind a permission flag.\n\nRalph gives a false pass on these. Ralph-Brother catches them.\n\nOnce Ralph-Brother finds this disconnect, it rewrites the fix plan to include BOTH the code gap AND the missing UI — and passes both to the fixer at once.")
    return s


def s_brother_codefix(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '03 — Ralph-Brother  ›  Fixing code and screen at once', ORG)

    t = T(title="ralph-brother — fixing everything in one go")
    t.o("  Fixing the code AND the screen at the same time...", DIM)
    t.blank()
    t.o("  Code side:", DIM)
    t.multi(('    Completing the filter logic               ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.multi(('    Adding the database query                 ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.blank()
    t.o("  Screen side:", DIM)
    t.multi(('    Creating the filter dropdown button       ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.multi(('    Placing it in the invoice toolbar         ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.multi(('    Options: Draft · Sent · Paid · Overdue    ', TXT), ('Done   ', GRN), ('✓', GRN))
    t.blank()
    t.sep()
    t.ok("  Both the code and the screen are fixed!")
    t.o("  Now checking if a user can actually find it...", DIM)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph-Brother fixing both layers at once.\n\nBecause Ralph-Brother already identified both the code gap and the missing UI button in the previous step, the fixer can build everything in a single pass:\n\n1. Code side first (always done before screen side):\n   - Complete the filter logic that was only partly there\n   - Add the database query to retrieve filtered invoices\n\n2. Screen side after:\n   - Create the filter dropdown component\n   - Place it in the invoice toolbar so users can see it\n   - Wire the options to the filter logic\n\nRalph would have only done the code side. Users would still not be able to find the feature.")
    return s


def s_brother_uicheck(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '03 — Ralph-Brother  ›  Checking the live screen', ORG)

    t = T(title="ralph-brother — checking the app like a real user")
    t.o("  Checking the app like a real user would...", DIM)
    t.blank()
    t.multi(('  Going to:  ', DIM), ('Invoicing  →  Invoices  →  Invoice List', CYN))
    t.blank()
    t.o('  Looking for:  "sort by date"', DIM)
    t.multi(('    Found the sort button in the table header       ', TXT), ('Visible   ', GRN), ('✓', GRN))
    t.o("    A user can click the column header to sort their invoices", DIM)
    t.blank()
    t.o('  Looking for:  "filter by status"', DIM)
    t.multi(('    Found the filter dropdown in the toolbar        ', TXT), ('Visible   ', GRN), ('✓', GRN))
    t.o("    Options:  Draft  ·  Sent  ·  Paid  ·  Overdue", DIM)
    t.blank()
    t.sep()
    t.ok("  A real user can find and use both features.")
    t.ok("  CONFIRMED  ✓")
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Verifying with a real user's perspective.\n\nAfter fixing, Ralph-Brother doesn't just trust that the fix worked. It opens the app and checks from the perspective of an actual user:\n\n1. It loads the navigation map — all the pages and menus in the app\n2. It traces the click path to reach the feature (Invoicing → Invoices → Invoice List)\n3. It looks for each specific thing a user needs to be able to do\n4. It checks that the element is actually on screen and interactable\n\nOnly when ALL requirements are confirmed visible does it mark the task as done.\n\nIf something is still missing it loops back and tries again (up to 3 attempts). This is what Ralph cannot do — Ralph never opens the app.")
    return s


def s_brother_done(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '03 — Ralph-Brother  ›  Done', ORG)

    t = T(title="ralph-brother — all done")
    t.dim("  [ ... ralph-brother checks 30 more tasks ... ]")
    t.blank()
    t.ok("  Task 29:  users can see it  ✓")
    t.ok("  Task 30:  users can see it  ✓  (needed 2 attempts)")
    t.warn("  Task 31:  needs a developer  ⚠  (requires a database change)")
    t.blank()
    t.sep()
    t.o("  ╔═════════════════════════════════════════════╗", ORG)
    t.o("  ║  Ralph-Brother is done!                     ║", ORG)
    t.o("  ║                                             ║", ORG)
    t.multi(('  ║  ', ORG), ('29 tasks:  Users can see & use them   ', WHT), ('✓  ║', ORG))
    t.multi(('  ║  ', ORG), (' 3 tasks:  Need a developer to help   ', YLW), ('⚠  ║', ORG))
    t.multi(('  ║  ', ORG), (' 0 tasks:  Still hidden from users    ', DIM), ('✗  ║', ORG))
    t.o("  ╚═════════════════════════════════════════════╝", ORG)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Ralph-Brother completing.\n\nFinal result:\n- 29 out of 32 tasks: features are confirmed visible and usable in the browser\n- 3 tasks: need a human developer (typically a database schema change or a decision about how something should look that the AI shouldn't make alone)\n- 0 tasks still hidden: everything was either fixed or escalated\n\nTo put it simply: before Ralph-Brother ran, 32 features existed in the code but users couldn't fully see them. After Ralph-Brother ran, 29 of those are now fully visible.\n\nThe 3 flagged items have a note explaining exactly what a developer needs to do.")
    return s


# ── 04: UI-Loop ──────────────────────────────────────────────────────────────
def s_uiloop(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '04 — UI-Loop  ›  Scanning what is already visible', PUR)

    t = T(title="ui-loop — scanning the app")
    t.prompt("start ui-loop")
    t.blank()
    t.o("  Scanning all 52 features at once...", DIM)
    t.blank()
    t.multi(('  "Invoice filter by status"       ', TXT), ('→  Partially visible   ', YLW), ('⚠', YLW))
    t.multi(('  "E-invoice order types"          ', TXT), ('→  Fully visible       ', GRN), ('✓', GRN))
    t.multi(('  "Invoice template selector"      ', TXT), ('→  Nothing on screen   ', RED), ('✗', RED))
    t.multi(('  "Shared service events"          ', TXT), ('→  Fully visible       ', GRN), ('✓', GRN))
    t.multi(('  "Bulk invoice actions"           ', TXT), ('→  Fully visible       ', GRN), ('✓', GRN))
    t.dim("  ...  (47 more features checked)")
    t.blank()
    t.o("  Also checking for old pages that no feature uses...", DIM)
    t.warn("    Found 2 old pages nobody visits anymore:")
    t.multi(('    → ', GRY), ('Old archive page     ', CYN), ('(leftover from 6 months ago)', DIM))
    t.multi(('    → ', GRY), ('Legacy report page   ', CYN), ('(nobody uses this anymore)',   DIM))
    t.blank()
    t.sep()
    t.ok("  Done:  46 fully visible  ·  4 partly visible  ·  2 hidden")
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "UI-Loop — the prerequisite scan.\n\nUI-Loop must run BEFORE Ralph-Brother. It produces the map that Ralph-Brother reads in its Step 2.\n\nWhat it does:\n1. Goes through all 52 features simultaneously (in parallel, not one at a time)\n2. For each feature it opens the app and looks for the corresponding buttons, screens, and interactions\n3. Gives a verdict: Fully visible / Partly visible / Nothing on screen\n4. Also scans for 'ghost pages' — pages in the app that have no feature backing them. These are often old forgotten code that should be deleted.\n\nResult in this run:\n- 46 features: users can already see them fine\n- 4 features: partially visible (some bits missing)\n- 2 features: not visible at all\n- 2 ghost pages found\n\nRalph-Brother then only works on the 4 + 2 = 6 problem features (plus any code gaps Ralph found).")
    return s


# ── 05: Full Pipeline ────────────────────────────────────────────────────────
def pipeline_img():
    W, H = 1600, 560
    im = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(im)

    fn = sansb(26); fd = sans(18); fs = sans(15)
    bw, bh = 230, 64

    # Layout: ui-loop → (ralph + brother) → ui-loop confirm
    steps = [
        (175, H//2,         '/ui-loop',         PUR, '① Scan the app'),
        (560, H//2 - 85,    '/ralph',            GRN, '② Fix the code'),
        (560, H//2 + 85,    '/ralph-brother',    ORG, '③ Fix the screen'),
        (945, H//2,         '/ui-loop',          PUR, '④ Confirm'),
    ]

    import math
    def arrow(x1, y1, x2, y2, c):
        d.line([x1, y1, x2, y2], fill=c, width=2)
        ang = math.atan2(y2-y1, x2-x1)
        for a in [ang+2.5, ang-2.5]:
            d.line([x2, y2, x2-int(11*math.cos(a)), y2-int(11*math.sin(a))], fill=c, width=2)

    # Arrows
    arrow(175+bw//2, H//2-12, 560-bw//2, H//2-85, GRY)
    arrow(175+bw//2, H//2+12, 560-bw//2, H//2+85, GRY)
    arrow(560+bw//2, H//2-85, 945-bw//2, H//2-12, GRY)
    arrow(560+bw//2, H//2+85, 945-bw//2, H//2+12, GRY)

    # Vertical brace line for parallel
    d.line([545, H//2-110, 545, H//2+110], fill=GRY, width=1)
    d.text((560, H//2-118), 'run at the same time', fill=GRY, font=fs, anchor='lm')

    # Boxes
    for x, y, label, color, sub in steps:
        r = [x-bw//2, y-bh//2, x+bw//2, y+bh//2]
        d.rounded_rectangle(r, radius=10, fill=NAVY, outline=color, width=2)
        d.text((x, y-12), label, fill=color, font=fn, anchor='mm')
        d.text((x, y+16), sub, fill=DIM, font=fd, anchor='mm')

    # Notes below boxes
    notes_text = [
        (175,  H//2+bh//2+28, 'Maps what users\ncan already see', PUR),
        (560,  H//2+bh//2+112, 'Fixes missing\ncode & features', GRN),
        (945,  H//2+bh//2+28, 'Confirms everything\nis now visible', PUR),
    ]
    for x, y, t, c in notes_text:
        for i, line in enumerate(t.split('\n')):
            d.text((x, y + i*22), line, fill=c, font=fs, anchor='mm')

    d.rectangle([0,0,W-1,H-1], outline=BDR, width=1)
    return im


def s_pipeline(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '05 — All Together  ›  The pipeline', CYN)

    pi = pipeline_img()
    img(s, pi, Inches(0.5), Inches(1.0), w=Inches(12.3))

    note(s, "The Full Pipeline — how to run it.\n\nStep ① — UI-Loop first\nScan the app to see what users can already see. This takes about 10-20 minutes and produces a report that Ralph-Brother will read.\n\nStep ② and ③ — Ralph + Ralph-Brother at the same time\nThey don't interfere with each other, so run them in parallel:\n- Ralph fixes everything where the code is broken or missing\n- Ralph-Brother fixes everything where users can't see or use the feature\n\nStep ④ — UI-Loop again to confirm\nRun UI-Loop one more time to get a clean final report. This confirms that all of Ralph-Brother's screen fixes actually worked.\n\nTotal time: roughly 4-6 hours automated vs 5+ days manual for 52 features.")
    return s


def files_img():
    W, H = 1600, 520
    im = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(im)
    fp = mono(17); fd = sans(15)

    files = [
        ('task list',              PUR, 'The 52 features that need to be built.\nAll tools read this. All tools write their results back here.'),
        ('code gap reports',       GRN, 'What was missing in the code.\nOne report per feature. Written by Ralph.'),
        ('screen reports',         PUR, 'What is visible in the app.\nOne report per feature. Written by UI-Loop.'),
        ('screen coverage summary',PUR, 'Full overview + list of old ghost pages.\nWritten by UI-Loop when all checks finish.'),
        ('ralph log',              GRN, 'A running log of every action Ralph took.\nNever deleted — always grows.'),
        ('ralph summary',          GRN, "Final results from Ralph's run.\nPassing / Needs help / Still broken."),
        ('ralph-brother log',      ORG, 'A running log of every action Ralph-Brother took.\nNever deleted — always grows.'),
        ('ralph-brother summary',  ORG, "Final results from Ralph-Brother's run.\nVisible / Needs help / Still hidden."),
    ]

    pad, gx, gy = 28, 36, 20
    cols = 4
    bw = (W - pad*2 - gx*(cols-1)) // cols
    bh = (H - pad*2 - gy) // 2

    for i, (name, color, desc) in enumerate(files):
        col = i % cols; row = i // cols
        x = pad + col*(bw+gx); y = pad + row*(bh+gy)
        d.rounded_rectangle([x,y,x+bw,y+bh], radius=8, fill=CARD, outline=BDR, width=1)
        d.rounded_rectangle([x,y,x+6,y+bh], radius=4, fill=color, outline=color)
        d.text((x+18, y+12), name, fill=color, font=fp)
        d.line([x+12, y+40, x+bw-12, y+40], fill=BDR, width=1)
        dy = y + 52
        for ln in desc.split('\n'):
            d.text((x+18, dy), ln, fill=DIM, font=fd); dy += 22

    d.rectangle([0,0,W-1,H-1], outline=BDR, width=1)
    return im


def s_files(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '05 — All Together  ›  What gets produced', CYN)

    fi = files_img()
    img(s, fi, Inches(0.5), Inches(1.0), w=Inches(12.3))

    note(s, "Files produced — what you get at the end.\n\nEverything is saved in a 'ralph/' folder in the project:\n\n- Task list (prd.json): The living source of truth. Before the run it shows 0 features passing. After the run it shows 49+ features confirmed working.\n\n- Code gap reports: One markdown file per feature. Each one says exactly what was missing and what was built to fix it.\n\n- Screen reports: One markdown file per feature showing the click path a user takes and whether each requirement is visible.\n\n- Logs: Append-only logs for every action taken. Never deleted. Useful for auditing what happened and why.\n\n- Summaries: Human-readable results files — the thing you share with the team to show progress.\n\nAll of these are plain text files that any developer can read and understand.")
    return s


def summary_img():
    W, H = 1600, 380
    im = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(im)
    fn = sansb(22); fm = mono(52); fd = sans(18); fh = sansb(36)

    cards = [
        ('Ralph',         GRN, '49 / 52', 'tasks working',  'code verified', '~39 hrs → 4 hrs'),
        ('Ralph-Brother', ORG, '29 / 32', 'users can use it','UI confirmed',  'invisible → visible'),
        ('UI-Loop',       PUR, '46 / 52', 'features visible','screen mapped', '+ 2 ghost pages found'),
    ]

    pad = 28; gx = 36
    bw = (W - pad*2 - gx*(len(cards)-1)) // len(cards)
    bh = H - pad*2

    for i, (label, color, stat, sub1, sub2, note_txt) in enumerate(cards):
        x = pad + i*(bw+gx); y = pad
        d.rounded_rectangle([x,y,x+bw,y+bh], radius=12, fill=CARD, outline=color, width=2)
        d.line([x, y+58, x+bw, y+58], fill=BDR, width=1)
        d.text((x+bw//2, y+32), label, fill=color, font=fn, anchor='mm')
        d.text((x+bw//2, y+118), stat, fill=color, font=fm, anchor='mm')
        d.text((x+bw//2, y+172), sub1, fill=WHT, font=fd, anchor='mm')
        d.text((x+bw//2, y+200), sub2, fill=DIM, font=fd, anchor='mm')
        d.line([x+24, y+230, x+bw-24, y+230], fill=BDR, width=1)
        d.text((x+bw//2, y+260), note_txt, fill=GRY, font=fd, anchor='mm')

    d.rectangle([0,0,W-1,H-1], outline=BDR, width=1)
    return im


def s_summary(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    box(s, 0, 0, SW, Inches(0.06), GRN)

    txt(s, 'Results', Inches(0.8), Inches(0.28), Inches(6), Inches(0.68),
        sz=32, color=WHT, bold=True)

    si = summary_img()
    img(s, si, Inches(0.5), Inches(1.1), w=Inches(12.3))

    txt(s, 'From manual checking (~5 days) to fully automated verification (4–6 hours)',
        Inches(0.5), Inches(6.82), Inches(12.3), Inches(0.45),
        sz=16, color=DIM, wrap=False, align=PP_ALIGN.CENTER)

    box(s, 0, SH - Inches(0.06), SW, Inches(0.06), GRN)

    note(s, "Summary.\n\nRalph: 49 out of 52 tasks confirmed working in the code. What used to take ~39 hours of manual checking now takes about 4 hours automated.\n\nRalph-Brother: 29 out of 32 tasks confirmed visible and usable by real users. Features that existed in the code but were hidden from users are now properly connected to the screen.\n\nUI-Loop: 46 out of 52 features mapped to visible pages, plus 2 ghost pages identified for cleanup.\n\nThe 3 items flagged for human attention in each tool have clear notes explaining exactly what a developer needs to do — no guessing.\n\nThe biggest win: Ralph-Brother closes the gap between 'the code exists' and 'users can actually use it'. That gap is surprisingly large on any real project.")
    return s


# ── 06: Discovery — Invoicing ────────────────────────────────────────────────

def s_discovery_requirements(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '06 — Discovery  ›  The starting point', CYN)

    t = T(title="invoicing — 52 requirements, zero code")
    t.o("  The brief:   build a complete invoicing system from scratch.", WHT)
    t.o("  Customer:    a waste management company in Finland.", DIM)
    t.blank()
    t.o("  What existed on day one:", DIM)
    t.multi(('    ✓  ', GRN), ('52 requirements tracked as Linear tickets (PD-xxx)',  WHT))
    t.multi(('    ✗  ', RED), ('No existing code',                                    DIM))
    t.multi(('    ✗  ', RED), ('No data model or architecture docs',                  DIM))
    t.multi(('    ✗  ', RED), ('No clear order to build things in',                   DIM))
    t.blank()
    t.o("  The requirements ranged widely in complexity:", DIM)
    t.blank()
    t.multi(('    Simple:   ', GRN), ('"Define VAT rates per product"',                         CYN))
    t.multi(('    Medium:   ', YLW), ('"Simulate an invoice run before sending it"',             CYN))
    t.multi(('    Complex:  ', RED), ('"Split shared costs across 4 neighbours — must = 100%"',  CYN))
    t.blank()
    t.sep()
    t.warn("  The first job was not writing code.")
    t.warn("  It was understanding how 52 requirements connect.")
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Discovery — the starting point.\n\nOn day one of the Invoicing project we had 52 requirements in Linear.\n\nNo code. No database schema. No architecture document. No clear picture of what to build first.\n\nThe requirements ranged from simple config features (VAT rates) to genuinely complex business logic (splitting shared service costs across multiple neighbours where the total must be exactly 100%).\n\nBefore writing a single line of code, the first job was to read all 52 requirements and understand how they fit together. Which features depend on which? What breaks if you build in the wrong order? What is the simplest possible version that still works?")
    return s


def s_discovery_approach(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '06 — Discovery  ›  Tools & approach', CYN)

    t = T(title="invoicing — turning 52 requirements into a build plan")
    t.o("  Tool used:  Claude Code  (Claude AI running inside the terminal)", WHT)
    t.blank()
    t.o("  Approach:   feed all 52 requirements in, ask it to find", DIM)
    t.o("              dependencies and produce a structured build order.", DIM)
    t.blank()
    t.o("  Output — 7 structured documents:", DIM)
    t.blank()
    t.multi(('    01  ', CYN), ('Domain model         ', WHT), ('all entities and their relationships',    DIM))
    t.multi(('    02  ', CYN), ('Data layer           ', WHT), ('every table, column, and index needed',   DIM))
    t.multi(('    03  ', CYN), ('Business logic       ', WHT), ('the rules that live inside each service', DIM))
    t.multi(('    04  ', CYN), ('API layer            ', WHT), ('every endpoint with request and response', DIM))
    t.multi(('    05  ', CYN), ('Integration layer    ', WHT), ('FINVOICE, external system calls',         DIM))
    t.multi(('    06  ', CYN), ('Cross-cutting rules  ', WHT), ('security, simulation guard, locking',     DIM))
    t.multi(('    07  ', CYN), ('Build order          ', WHT), ('13 steps, each with "why this position"', DIM))
    t.blank()
    t.sep()
    t.multi(('  Time with Claude:   ', DIM), ('~4 hours',                GRN))
    t.multi(('  Time without:       ', DIM), ('2–3 days of manual work', RED))
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Discovery approach.\n\nInstead of manually reading 52 requirements and trying to build a dependency map in your head, we used Claude Code as the analytical engine.\n\nThe prompt was essentially: here are all the requirements, find the dependencies, identify what must be built first, and produce structured documents for each layer of the system.\n\nEach document was grounded in the actual requirement text — with quotes from the Linear tickets explaining WHY each architectural decision was made.\n\nThis took about 4 hours instead of the 2-3 days it would normally take to produce equivalent analysis manually.")
    return s


def s_discovery_result(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '06 — Discovery  ›  From docs to prototype', CYN)

    t = T(title="invoicing — following the build order")
    t.o("  Tech stack:", DIM)
    t.multi(('    Backend   ', GRN), ('Spring Boot + JPA + PostgreSQL',  WHT))
    t.multi(('    Frontend  ', GRN), ('React + Vite + Material UI',      WHT))
    t.blank()
    t.o("  Built in sequence, following the 13 steps:", DIM)
    t.blank()
    t.multi(('    Weeks 1–2  ', YLW), ('Steps  1–3   master data + billing events  ', WHT), ('core entities live',    GRN))
    t.multi(('    Week 3     ', YLW), ('Steps  4–6   corrections + billing config  ', WHT), ('pre-billing complete',  GRN))
    t.multi(('    Weeks 4–5  ', YLW), ('Steps  7–9   invoice generation + run      ', WHT), ('end-to-end flow works', GRN))
    t.multi(('    Week 6     ', YLW), ('Steps 10–13  credit notes + integration    ', WHT), ('full MVP done',         GRN))
    t.blank()
    t.o("  Ralph verified requirements automatically after each step.", DIM)
    t.o("  Bugs caught early, before they could compound.", DIM)
    t.blank()
    t.sep()
    t.multi(('  Final result:  ', DIM), ('49 / 52 requirements passing  ', GRN), ('✓', GRN))
    t.multi(('                 ', DIM), (' 3 / 52 flagged for human review', YLW), ('⚠', YLW))
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "From docs to prototype.\n\nWith the structured documents and 13-step build order in hand, the prototype was built systematically over 6 weeks.\n\nKey insight from the build order: steps 4, 5, and 6 can be parallelised once step 3 is done — they don't depend on each other. This saved a week of sequential work.\n\nRalph ran throughout the process. After each major step it checked all in-scope requirements automatically. Bugs were caught early, before they had a chance to compound.\n\nFinal result: 49 of 52 requirements passing automatically. The 3 flagged items all involved database migrations or architectural decisions that needed a human to approve.")
    return s


# ── 07: Planning — P&P ───────────────────────────────────────────────────────

def s_pp_grooming(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '07 — Planning P&P  ›  Grooming & breaking down tasks', YLW)

    t = T(title="p&p — grooming and breaking down tasks")
    t.o("  Grooming is not estimation.", WHT)
    t.o("  Grooming is making sure everyone understands the same thing", WHT)
    t.o("  before anyone tries to estimate.", DIM)
    t.blank()
    t.o("  In a grooming session we ask:", DIM)
    t.multi(('    → ', GRY), ('"What would break if we got this wrong?"',          WHT))
    t.multi(('    → ', GRY), ('"What is the simplest version that still works?"',  WHT))
    t.multi(('    → ', GRY), ('"Are there unknowns we need to investigate first?"',WHT))
    t.blank()
    t.o("  After grooming: break each requirement into individual tasks.", DIM)
    t.blank()
    t.o('  Example:  "Products have names in Finnish, Swedish, English"', CYN)
    t.blank()
    t.multi(('    BE-1  ', YLW), ('Add ProductTranslation table + migration',   DIM))
    t.multi(('    BE-2  ', YLW), ('POST /products — accept translations',        DIM))
    t.multi(('    BE-3  ', YLW), ('GET  /products — resolve by language code',   DIM))
    t.multi(('    FE-1  ', CYN), ('Product form: language name input fields',    DIM))
    t.multi(('    FE-2  ', CYN), ('Product list: display in user language',      DIM))
    t.blank()
    t.sep()
    t.o("  Rule:  if you cannot estimate a task confidently, it is still an epic.", RED)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Grooming and breaking down tasks.\n\nGrooming is the step where the team reads each requirement together and makes sure everyone understands the same thing. The biggest waste in planning is estimating something nobody actually understands.\n\nGrooming sessions should be focused on understanding, not implementation. If something is unclear or unknown, it becomes a spike task — a time-boxed investigation with a specific question to answer.\n\nAfter grooming, we break each requirement into individual tasks split by layer (BE/FE) and by operation. Each task has a clear done condition.\n\nThe 'products in multiple languages' example goes from one vague epic to five specific tasks, each independently completable by different people.\n\nThe rule: if you can't estimate a task confidently, it's still an epic. Break it down further.")
    return s


def s_pp_apicontracts(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '07 — Planning P&P  ›  API contracts', YLW)

    t = T(title="what is an api contract?")
    t.o("  An API contract is the agreement between backend and frontend.", WHT)
    t.o("  Before any code is written, both sides agree on the shape of the data.", DIM)
    t.blank()
    t.o("  Think of it like a building blueprint:", DIM)
    t.multi(('    Blueprint  ', DIM), ('→  architects and builders agree on walls before construction', DIM))
    t.multi(('    Contract   ', GRN), ('→  FE and BE agree on data shape before any code is written',  WHT))
    t.blank()
    t.o('  Example — contract for creating a product:', DIM)
    t.blank()
    t.multi(('    Endpoint:  ', DIM), ('POST /products',                                      CYN))
    t.multi(('    Request:   ', DIM), ('{ "name": {"fi": "...", "sv": "..."}, "vatId": "uuid" }', WHT))
    t.multi(('    Success:   ', DIM), ('201 Created  +  { "id": "uuid", "name": {...} }',     GRN))
    t.multi(('    Error:     ', DIM), ('400  +  { "code": "MISSING_NAME", "field": "name.fi" }', RED))
    t.blank()
    t.o("  With this contract in place:", DIM)
    t.multi(('    ✓  ', GRN), ('Backend builds the endpoint independently',   WHT))
    t.multi(('    ✓  ', GRN), ('Frontend builds the form independently',      WHT))
    t.multi(('    ✓  ', GRN), ('Integration works first time, no surprises',  WHT))
    t.blank()
    t.sep()
    t.multi(('  Writing a contract:       ', DIM), ('~30 min per feature', GRN))
    t.multi(('  Debugging without one:    ', DIM), ('hours',               RED))
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "API contracts.\n\nAn API contract is the agreement that lets frontend and backend develop in parallel without stepping on each other.\n\nBefore anyone writes code, both teams sit down and agree:\n- What is the URL?\n- What goes in the request body (field names, types, required vs optional)?\n- What comes back in the response?\n- What error codes and messages can the caller expect?\n\nOnce the contract is agreed, both teams can work independently. The integration works first time because both sides were building to the same spec.\n\nWithout a contract: the frontend sends 'productId' but the backend expects 'product_id'. The backend returns a 500 with no message and the frontend has no idea what went wrong.\n\nWriting contracts takes about 30 minutes per feature. It saves hours of debugging at integration time.")
    return s


def s_pp_estimation(prs, layout):
    s = prs.slides.add_slide(layout); set_bg(s, BG)
    tag(s, '07 — Planning P&P  ›  Estimation', YLW)

    t = T(title="p&p — estimation: points measure complexity, not days")
    t.o("  Story points measure complexity and risk, not hours.", WHT)
    t.blank()
    t.o("  We estimate AFTER writing the API contract —", DIM)
    t.o("  because the contract removes hidden unknowns.", DIM)
    t.blank()
    t.o("  Our scale:", DIM)
    t.blank()
    t.multi(('    1 point  ', GRN), ('I have done this exact thing before.  Zero unknowns.', DIM))
    t.multi(('    2 points ', GRN), ('Similar to things I know.  Small edge cases possible.', DIM))
    t.multi(('    3 points ', YLW), ('Understood but has several moving parts.',              DIM))
    t.multi(('    5 points ', YLW), ('More complex.  Could hit something unexpected.',         DIM))
    t.multi(('    8 points ', RED), ('Many unknowns or many dependencies.  Split it.',         DIM))
    t.blank()
    t.o("  The disagreement rule:", DIM)
    t.blank()
    t.multi(('  Person A says 2.  Person B says 8.  ', DIM), ('Do not average.', YLW))
    t.o("  Stop.  Talk.  One of you knows something the other does not.", DIM)
    t.o("  Resolve the unknown.  Re-estimate.  Now you both say 3.", GRN)
    t.blank()
    t.sep()
    t.o("  A big disagreement is not a problem — it is information.", DIM)
    t.sep()

    i = t.render()
    img(s, i, Inches(0.5), Inches(1.0), h=Inches(6.15))
    note(s, "Estimation.\n\nStory points measure complexity and risk, not time. The reason we don't estimate in hours is that hours are different for everyone — a senior developer might do something in 2 hours that takes a junior 2 days. Points abstract away seniority.\n\nThe critical rule: estimate AFTER the API contract is written. With a contract in hand, the task is well-defined enough that estimates are reliable. Estimating a vague description produces wildly different numbers that tell you nothing about complexity — they just reflect confusion.\n\nWhen two people estimate very differently (2 vs 8), that's the most valuable thing in the planning session. Stop and discuss. One person knows something the other doesn't. Once you find and resolve that unknown, you usually converge on the same estimate.")
    return s


# ═══════════════════════════════════════════════════════════════════════════
#  MAIN
# ═══════════════════════════════════════════════════════════════════════════

def main():
    prs = Presentation()
    prs.slide_width  = SW
    prs.slide_height = SH
    blank = prs.slide_layouts[6]

    print('Building slides...')

    s_title(prs, blank);           print('  01  Title')
    s_agenda(prs, blank);          print('  02  Agenda')

    s_section(prs, blank, 1, 'The Problem',
              RED, '52 features. Checking by hand takes days. Mistakes are guaranteed.');  print('  03  Section: Problem')
    s_problem(prs, blank);         print('  04  The Problem')

    s_section(prs, blank, 2, 'Ralph',
              GRN, 'An AI that reads the task list, checks each feature, and builds what is missing.'); print('  05  Section: Ralph')
    s_ralph_start(prs, blank);     print('  06  Ralph start')
    s_ralph_gapcheck(prs, blank);  print('  07  Checking a task')
    s_ralph_codefix(prs, blank);   print('  08  Building the fix')
    s_ralph_done(prs, blank);      print('  09  Ralph done')

    s_section(prs, blank, 3, 'Ralph-Brother',
              ORG, 'Same as Ralph — but also checks if real users can find and use each feature.'); print('  10  Section: Brother')
    s_brother_start(prs, blank);     print('  11  Brother start')
    s_brother_synthesis(prs, blank); print('  12  The key difference')
    s_brother_codefix(prs, blank);   print('  13  Fixing both layers')
    s_brother_uicheck(prs, blank);   print('  14  Checking the screen')
    s_brother_done(prs, blank);      print('  15  Brother done')

    s_section(prs, blank, 4, 'UI-Loop',
              PUR, 'Scans the app first so Ralph-Brother knows what is already visible.'); print('  16  Section: UI-Loop')
    s_uiloop(prs, blank);            print('  17  UI-Loop')

    s_section(prs, blank, 5, 'All Together',
              CYN, 'Run them in the right order — the whole product gets built and verified.'); print('  18  Section: Pipeline')
    s_pipeline(prs, blank);          print('  19  Pipeline diagram')
    s_files(prs, blank);             print('  20  Files produced')
    s_summary(prs, blank);           print('  21  Summary')

    s_section(prs, blank, 6, 'Discovery',
              CYN, 'From 52 requirements to a functional prototype.');  print('  22  Section: Discovery')
    s_discovery_requirements(prs, blank); print('  23  Discovery: starting point')
    s_discovery_approach(prs, blank);     print('  24  Discovery: tools & approach')
    s_discovery_result(prs, blank);       print('  25  Discovery: prototype outcome')

    s_section(prs, blank, 7, 'Planning P&P',
              YLW, 'Grooming, task breakdown, API contracts, and estimation.'); print('  26  Section: Planning P&P')
    s_pp_grooming(prs, blank);            print('  27  P&P: grooming & breakdown')
    s_pp_apicontracts(prs, blank);        print('  28  P&P: API contracts')
    s_pp_estimation(prs, blank);          print('  29  P&P: estimation')

    out = '/mnt/c/Users/drasm/Desktop/Invoicing/ralph-presentation.pptx'
    prs.save(out)
    print(f'\nSaved → {out}')

if __name__ == '__main__':
    main()
