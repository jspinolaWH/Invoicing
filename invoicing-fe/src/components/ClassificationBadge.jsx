export default function ClassificationBadge({ classification }) {
  if (!classification) return <span className="badge badge-grey">Unclassified</span>
  if (classification === 'PUBLIC_LAW') return <span className="badge badge-blue">Public Law</span>
  if (classification === 'PRIVATE_LAW') return <span className="badge badge-orange">Private Law</span>
  return <span className="badge badge-grey">{classification}</span>
}
