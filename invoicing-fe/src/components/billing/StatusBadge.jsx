import styles from './StatusBadge.module.css'

const STATUS_CONFIG = {
  IN_PROGRESS: { label: 'In Progress', cls: 'in-progress' },
  SENT:        { label: 'Sent',        cls: 'sent'        },
  COMPLETED:   { label: 'Completed',   cls: 'completed'   },
  ERROR:       { label: 'Error',       cls: 'error'       },
}

export default function StatusBadge({ status }) {
  const cfg = STATUS_CONFIG[status] ?? { label: status, cls: 'default' }
  return (
    <span className={`${styles.badge} ${styles[cfg.cls] ?? styles.default}`}>
      {cfg.label}
    </span>
  )
}
