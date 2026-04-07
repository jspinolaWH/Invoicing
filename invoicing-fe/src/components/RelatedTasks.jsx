/**
 * RelatedTasks — shows PD/step task references below the page header.
 *
 * Usage:
 *   <RelatedTasks tasks={[
 *     { id: 'STEP-01', label: 'Foundation Setup + VAT Rates' },
 *   ]} />
 *
 * Add `href` to make the badge a link (e.g. pointing to a Jira ticket or doc).
 */
export default function RelatedTasks({ tasks = [] }) {
  if (!tasks.length) return null

  return (
    <div className="related-tasks">
      <span className="related-tasks-label">Related tasks</span>
      <div className="related-tasks-list">
        {tasks.map((t) =>
          t.href ? (
            <a
              key={t.id}
              href={t.href}
              target="_blank"
              rel="noreferrer"
              className="task-badge task-badge--link"
            >
              <span className="task-badge-id">{t.id}</span>
              <span className="task-badge-sep">·</span>
              <span className="task-badge-label">{t.label}</span>
            </a>
          ) : (
            <span key={t.id} className="task-badge">
              <span className="task-badge-id">{t.id}</span>
              <span className="task-badge-sep">·</span>
              <span className="task-badge-label">{t.label}</span>
            </span>
          )
        )}
      </div>
    </div>
  )
}
