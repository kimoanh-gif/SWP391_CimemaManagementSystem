document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-paged-section]").forEach((section) => {
    const items = [...section.querySelectorAll(".pn-page-item")];
    const pageSize = Number(section.dataset.pageSize) || 3;
    const pager = section.querySelector(".pn-pager");
    const previous = section.querySelector(".pn-page-prev");
    const next = section.querySelector(".pn-page-next");
    const status = section.querySelector(".pn-page-status");
    const pageCount = Math.ceil(items.length / pageSize);
    let page = 0;

    if (!pager || pageCount <= 1) {
      if (pager) pager.hidden = true;
      return;
    }

    const render = () => {
      const first = page * pageSize;
      items.forEach((item, index) => {
        item.hidden = index < first || index >= first + pageSize;
      });
      previous.disabled = page === 0;
      next.disabled = page === pageCount - 1;
      status.textContent = `${page + 1} / ${pageCount}`;
    };

    previous.addEventListener("click", () => {
      if (page > 0) {
        page -= 1;
        render();
      }
    });
    next.addEventListener("click", () => {
      if (page < pageCount - 1) {
        page += 1;
        render();
      }
    });
    render();
  });
});
