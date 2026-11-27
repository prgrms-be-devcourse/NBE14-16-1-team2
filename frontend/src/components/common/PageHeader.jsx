/**
 * 페이지 제목(h1)·설명·작업 버튼 영역.
 * 한 페이지에 한 번, 본문 맨 위에 둔다. actions에는 Button 등을 넣는다.
 */
function PageHeader({ title, description, actions }) {
  return (
    <div className="page-header">
      <div className="page-header__text">
        <h1 className="page-header__title text-break">{title}</h1>
        {description && <p className="page-header__description text-break">{description}</p>}
      </div>
      {actions && <div className="page-header__actions">{actions}</div>}
    </div>
  )
}

export default PageHeader
