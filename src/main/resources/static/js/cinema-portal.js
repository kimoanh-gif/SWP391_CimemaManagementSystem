/**
 * CineVerse Customer Portal JavaScript
 * Author: Nguyễn Đức Anh (Iter2)
 */

document.addEventListener('DOMContentLoaded', () => {
    initTrailerModal();
    initVoucherCopy();
    initStarRating();
    initQuickBooking();
});

/* =====================================================================
   1. TRAILER MODAL
   ===================================================================== */
function initTrailerModal() {
    const modal = document.getElementById('trailerModal');
    const iframe = document.getElementById('trailerIframe');
    const closeBtn = document.getElementById('closeTrailerBtn');

    if (!modal || !iframe) return;

    window.openTrailer = function(rawUrl) {
        if (!rawUrl) return;
        let embedUrl = rawUrl;
        if (rawUrl.includes('watch?v=')) {
            const videoId = rawUrl.split('watch?v=')[1].split('&')[0];
            embedUrl = `https://www.youtube.com/embed/${videoId}?autoplay=1`;
        } else if (rawUrl.includes('youtu.be/')) {
            const videoId = rawUrl.split('youtu.be/')[1].split('?')[0];
            embedUrl = `https://www.youtube.com/embed/${videoId}?autoplay=1`;
        } else if (!embedUrl.includes('autoplay=1')) {
            embedUrl += (embedUrl.includes('?') ? '&' : '?') + 'autoplay=1';
        }
        iframe.src = embedUrl;
        modal.classList.add('open');
        document.body.style.overflow = 'hidden';
    };

    window.closeTrailer = function() {
        modal.classList.remove('open');
        iframe.src = '';
        document.body.style.overflow = '';
    };

    if (closeBtn) {
        closeBtn.addEventListener('click', window.closeTrailer);
    }

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            window.closeTrailer();
        }
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('open')) {
            window.closeTrailer();
        }
    });
}

/* =====================================================================
   2. VOUCHER COPY TO CLIPBOARD & TOAST
   ===================================================================== */
function initVoucherCopy() {
    const toast = document.getElementById('cinemaToast');
    const toastMsg = document.getElementById('cinemaToastMsg');

    window.copyVoucherCode = function(code) {
        if (!code) return;
        navigator.clipboard.writeText(code).then(() => {
            showToast(`Đã sao chép mã ưu đãi "${code}"!`);
        }).catch(() => {
            showToast(`Mã ưu đãi: ${code}`);
        });
    };

    function showToast(message) {
        if (!toast || !toastMsg) return;
        toastMsg.textContent = message;
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3200);
    }
}

/* =====================================================================
   3. STAR RATING PICKER
   ===================================================================== */
function initStarRating() {
    const starContainer = document.getElementById('starRatingInput');
    const ratingHiddenInput = document.getElementById('ratingValue');

    if (!starContainer || !ratingHiddenInput) return;

    const stars = starContainer.querySelectorAll('.star-icon');
    stars.forEach(star => {
        star.addEventListener('click', () => {
            const val = parseInt(star.getAttribute('data-value'), 10);
            ratingHiddenInput.value = val;
            updateStars(val);
        });

        star.addEventListener('mouseenter', () => {
            const val = parseInt(star.getAttribute('data-value'), 10);
            highlightStars(val);
        });
    });

    starContainer.addEventListener('mouseleave', () => {
        const currentVal = parseInt(ratingHiddenInput.value, 10) || 5;
        updateStars(currentVal);
    });

    function highlightStars(val) {
        stars.forEach(s => {
            const sVal = parseInt(s.getAttribute('data-value'), 10);
            if (sVal <= val) {
                s.style.color = '#f59e0b';
            } else {
                s.style.color = '#475569';
            }
        });
    }

    function updateStars(val) {
        stars.forEach(s => {
            const sVal = parseInt(s.getAttribute('data-value'), 10);
            if (sVal <= val) {
                s.style.color = '#f59e0b';
            } else {
                s.style.color = '#475569';
            }
        });
    }
}

/* =====================================================================
   4. QUICK BOOKING BAR
   ===================================================================== */
function initQuickBooking() {
    const movieSelect = document.getElementById('quickMovieSelect');
    const dateSelect = document.getElementById('quickDateSelect');
    const submitBtn = document.getElementById('quickBookBtn');

    if (!movieSelect || !submitBtn) return;

    submitBtn.addEventListener('click', (e) => {
        const movieId = movieSelect.value;
        const date = dateSelect ? dateSelect.value : '';

        e.preventDefault();

        // No movie selected: open the Showtime List for the chosen date (all movies).
        if (!movieId) {
            window.location.href = date ? `/showtimes?date=${encodeURIComponent(date)}` : '/showtimes';
            return;
        }

        // Movie selected: open its showtimes on Movie Details for the chosen date.
        let targetUrl = `/movies/${encodeURIComponent(movieId)}`;
        if (date) {
            targetUrl += `?date=${encodeURIComponent(date)}`;
        }
        window.location.href = targetUrl + '#showtimes';
    });
}
