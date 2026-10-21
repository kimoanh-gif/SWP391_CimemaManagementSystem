/**
 * pagination-helper.js
 * Universal, responsive pagination utility for CINEORA Cinema Suites tables and grids.
 */
class CineoraPaginator {
    constructor(options) {
        this.container = typeof options.container === 'string'
            ? document.querySelector(options.container)
            : options.container;
        this.itemSelector = options.itemSelector || 'tr';
        this.controlsContainer = typeof options.controlsContainer === 'string'
            ? document.querySelector(options.controlsContainer)
            : options.controlsContainer;
        this.pageSize = options.pageSize || 8;
        this.itemName = options.itemName || 'items';
        this.currentPage = options.initialPage || 1;
        this.onPageChange = options.onPageChange || null;

        if (!this.container || !this.controlsContainer) {
            return;
        }

        this.init();
    }

    init() {
        this.update();
    }

    getItems() {
        if (!this.container) return [];
        // Only consider elements directly matching selector that are not empty-state placeholders
        return Array.from(this.container.querySelectorAll(this.itemSelector))
            .filter(el => !el.classList.contains('empty-state-row') && !el.closest('tfoot'));
    }

    getTotalPages() {
        const total = this.getItems().length;
        return Math.max(1, Math.ceil(total / this.pageSize));
    }

    setPage(page) {
        const totalPages = this.getTotalPages();
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        this.currentPage = page;
        this.render();
        if (this.onPageChange) {
            this.onPageChange(this.currentPage);
        }
    }

    update() {
        const totalPages = this.getTotalPages();
        if (this.currentPage > totalPages) {
            this.currentPage = totalPages;
        }
        this.render();
    }

    render() {
        const items = this.getItems();
        const totalItems = items.length;
        const totalPages = Math.max(1, Math.ceil(totalItems / this.pageSize));

        if (this.currentPage > totalPages) {
            this.currentPage = totalPages;
        }

        const startIndex = (this.currentPage - 1) * this.pageSize;
        const endIndex = Math.min(startIndex + this.pageSize, totalItems);

        // Hide or show items
        items.forEach((item, idx) => {
            if (idx >= startIndex && idx < endIndex) {
                item.style.display = '';
            } else {
                item.style.display = 'none';
            }
        });

        // If no items at all, hide pagination controls
        if (totalItems === 0) {
            this.controlsContainer.innerHTML = '';
            this.controlsContainer.classList.add('hidden');
            return;
        }

        this.controlsContainer.classList.remove('hidden');

        // Build pagination UI
        const displayStart = totalItems === 0 ? 0 : startIndex + 1;
        const displayEnd = endIndex;

        let pagesHtml = '';
        for (let i = 1; i <= totalPages; i++) {
            if (i === this.currentPage) {
                pagesHtml += `<button type="button" class="w-8 h-8 rounded-md font-label-sm text-xs font-semibold bg-cinema-red text-white shadow-sm transition-all" data-page="${i}">${i}</button>`;
            } else {
                pagesHtml += `<button type="button" class="w-8 h-8 rounded-md font-label-sm text-xs font-medium text-text-charcoal bg-surface-container hover:bg-surface-peach hover:text-cinema-red transition-all" data-page="${i}">${i}</button>`;
            }
        }

        this.controlsContainer.innerHTML = `
            <div class="w-full flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-4 mt-3 border-t border-[#EFE0D5]">
                <div class="flex items-center gap-2">
                    <span class="text-xs text-secondary">
                        Showing <span class="font-medium text-text-charcoal">${displayStart} – ${displayEnd}</span> of <span class="font-medium text-text-charcoal">${totalItems}</span> ${this.itemName}
                    </span>
                </div>
                <div class="flex items-center gap-1 self-end sm:self-auto">
                    <button type="button" class="btn-page-prev h-8 px-3 rounded-md text-xs font-medium text-secondary hover:bg-surface-peach hover:text-cinema-red disabled:opacity-40 transition-colors flex items-center gap-1" ${this.currentPage <= 1 ? 'disabled' : ''}>
                        <span class="material-symbols-outlined text-[16px]">chevron_left</span>
                        <span>Previous</span>
                    </button>
                    <div class="flex items-center gap-1">
                        ${pagesHtml}
                    </div>
                    <button type="button" class="btn-page-next h-8 px-3 rounded-md text-xs font-medium text-text-charcoal hover:bg-surface-peach hover:text-cinema-red disabled:opacity-40 transition-colors flex items-center gap-1" ${this.currentPage >= totalPages ? 'disabled' : ''}>
                        <span>Next</span>
                        <span class="material-symbols-outlined text-[16px]">chevron_right</span>
                    </button>
                </div>
            </div>
        `;

        // Event listeners
        const prevBtn = this.controlsContainer.querySelector('.btn-page-prev');
        if (prevBtn) {
            prevBtn.addEventListener('click', () => this.setPage(this.currentPage - 1));
        }

        const nextBtn = this.controlsContainer.querySelector('.btn-page-next');
        if (nextBtn) {
            nextBtn.addEventListener('click', () => this.setPage(this.currentPage + 1));
        }

        const pageBtns = this.controlsContainer.querySelectorAll('[data-page]');
        pageBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const targetPage = parseInt(e.currentTarget.getAttribute('data-page'), 10);
                if (!isNaN(targetPage)) {
                    this.setPage(targetPage);
                }
            });
        });
    }
}

// Global helper for easy initialization
window.initCineoraPagination = function (options) {
    return new CineoraPaginator(options);
};
