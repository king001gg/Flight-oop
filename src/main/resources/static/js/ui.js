export const $ = (selector, root = document) => root.querySelector(selector);
export const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

export function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (char) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[char]));
}

export function money(value) {
    return Number(value ?? 0).toFixed(2);
}

export function discountText(discount) {
    const value = Number(discount);
    if (!Number.isFinite(value) || value >= 0.999) {
        return '无折扣';
    }
    return `${(Math.round(value * 100) / 10).toString().replace(/\.0$/, '')}折`;
}

export function statusBadge(status) {
    return status === 'REFUNDED'
        ? '<span class="badge badge-danger">已退</span>'
        : '<span class="badge badge-success">已订</span>';
}

export function flightStatusBadge(full, remaining) {
    return full
        ? '<span class="badge badge-danger">满仓</span>'
        : `<span class="badge badge-success">可订</span>`;
}

export function levelBadge(level, text) {
    const cls = level === 'GOLD' ? 'badge-warning' : level === 'SILVER' ? 'badge-info' : 'badge-muted';
    return `<span class="badge ${cls}">${escapeHtml(text)}</span>`;
}

export function initials(name) {
    const text = String(name ?? '').trim();
    if (!text) {
        return '?';
    }
    return text.slice(0, 1).toUpperCase();
}

export function todayISO() {
    const now = new Date();
    const offset = now.getTimezoneOffset();
    return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 10);
}

export function firstDayOfMonthISO() {
    const date = new Date();
    date.setDate(1);
    const offset = date.getTimezoneOffset();
    return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 10);
}

export function lastDayOfMonthISO() {
    const date = new Date();
    date.setMonth(date.getMonth() + 1, 0);
    const offset = date.getTimezoneOffset();
    return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 10);
}

let toastStack = null;

export function toast(message, type = 'info', duration = 3400) {
    if (!toastStack) {
        toastStack = document.createElement('div');
        toastStack.className = 'toast-stack';
        document.body.appendChild(toastStack);
    }
    const element = document.createElement('div');
    element.className = `toast ${type}`;
    element.innerHTML = `
        <div class="toast-text">${escapeHtml(message)}</div>
        <button class="toast-close" aria-label="关闭">✕</button>`;
    toastStack.appendChild(element);
    element.querySelector('.toast-close').addEventListener('click', () => element.remove());
    window.setTimeout(() => {
        if (element.isConnected) {
            element.remove();
        }
    }, duration);
}

export function setButtonLoading(button, loading, loadingText = '处理中…') {
    if (!button) {
        return;
    }
    if (loading) {
        button.dataset.originalText = button.textContent;
        button.textContent = loadingText;
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalText || button.textContent;
        button.disabled = false;
    }
}

export function openModal(id) {
    const backdrop = document.getElementById(id);
    if (!backdrop) {
        return;
    }
    backdrop.classList.add('open');
    document.body.style.overflow = 'hidden';
    const focusable = backdrop.querySelector('input, select, textarea, button:not([data-close])');
    window.setTimeout(() => focusable?.focus(), 60);
}

export function closeModal(id) {
    const backdrop = document.getElementById(id);
    if (backdrop) {
        backdrop.classList.remove('open');
    }
    if (!document.querySelector('.modal-backdrop.open')) {
        document.body.style.overflow = '';
    }
}

export function closeAllModals() {
    $$('.modal-backdrop.open').forEach((modal) => modal.classList.remove('open'));
    document.body.style.overflow = '';
}

document.addEventListener('click', (event) => {
    const closeButton = event.target.closest?.('[data-close]');
    if (closeButton) {
        const modal = closeButton.closest('.modal-backdrop');
        if (modal) {
            closeModal(modal.id);
        }
        return;
    }
    if (event.target.classList?.contains('modal-backdrop')) {
        closeModal(event.target.id);
    }
});

document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') {
        return;
    }
    const openModals = $$('.modal-backdrop.open');
    if (openModals.length) {
        closeModal(openModals[openModals.length - 1].id);
    }
});

let confirmSequence = 0;

export function confirmAction({
    title = '请确认',
    message = '',
    confirmText = '确定',
    cancelText = '取消',
    danger = false
} = {}) {
    return new Promise((resolve) => {
        const id = `confirm-modal-${++confirmSequence}`;
        const backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop';
        backdrop.id = id;
        const safeMessage = String(message)
            .split('\n')
            .map(escapeHtml)
            .join('<br>');
        backdrop.innerHTML = `
            <div class="modal modal-sm" role="dialog" aria-modal="true">
                <div class="modal-header">
                    <h2>${escapeHtml(title)}</h2>
                    <button class="icon-btn" data-close aria-label="关闭">✕</button>
                </div>
                <div class="modal-body"><p class="muted">${safeMessage}</p></div>
                <div class="modal-footer">
                    <button class="btn btn-outline" data-cancel>${escapeHtml(cancelText)}</button>
                    <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-confirm>
                        ${escapeHtml(confirmText)}
                    </button>
                </div>
            </div>`;
        document.body.appendChild(backdrop);
        openModal(id);

        const finish = (value) => {
            document.removeEventListener('keydown', onKey);
            closeModal(id);
            backdrop.remove();
            resolve(value);
        };
        const onKey = (event) => {
            if (event.key === 'Escape') {
                finish(false);
            }
        };
        document.addEventListener('keydown', onKey);
        backdrop.querySelector('[data-confirm]').addEventListener('click', () => finish(true));
        backdrop.querySelector('[data-cancel]').addEventListener('click', () => finish(false));
        backdrop.querySelector('[data-close]').addEventListener('click', () => finish(false));
        backdrop.addEventListener('click', (event) => {
            if (event.target === backdrop) {
                finish(false);
            }
        });
    });
}

export function loadingHtml(text = '加载中…') {
    return `<div class="loading"><span class="spinner"></span>${escapeHtml(text)}</div>`;
}

export function emptyHtml(title, subtitle = '') {
    return `<div class="empty">
        <div class="empty-icon">✈</div>
        <strong>${escapeHtml(title)}</strong>
        <span>${escapeHtml(subtitle)}</span>
    </div>`;
}

export function paginate(items, page, pageSize) {
    const total = items.length;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    const current = Math.min(Math.max(1, page), totalPages);
    const start = (current - 1) * pageSize;
    return {
        items: items.slice(start, start + pageSize),
        page: current,
        totalPages,
        total,
        pageSize
    };
}

export function renderPagination(container, state, onChange) {
    if (!container) {
        return;
    }
    const { page, totalPages, total, pageSize } = state;
    if (!total) {
        container.innerHTML = '';
        return;
    }

    const numbers = [];
    if (totalPages <= 7) {
        for (let i = 1; i <= totalPages; i++) {
            numbers.push(i);
        }
    } else {
        numbers.push(1);
        if (page > 3) {
            numbers.push('...');
        }
        for (let i = Math.max(2, page - 1); i <= Math.min(totalPages - 1, page + 1); i++) {
            numbers.push(i);
        }
        if (page < totalPages - 2) {
            numbers.push('...');
        }
        numbers.push(totalPages);
    }

    container.innerHTML = `
        <div class="muted small">共 ${total} 条 · 每页 ${pageSize} 条</div>
        <div class="pages">
            <button class="page-btn" data-page="${page - 1}" ${page <= 1 ? 'disabled' : ''}>上一页</button>
            ${numbers.map((number) => number === '...'
                ? '<span class="page-btn" style="border:0;background:transparent;cursor:default">…</span>'
                : `<button class="page-btn ${number === page ? 'active' : ''}" data-page="${number}">${number}</button>`)
                .join('')}
            <button class="page-btn" data-page="${page + 1}" ${page >= totalPages ? 'disabled' : ''}>下一页</button>
        </div>`;

    container.querySelectorAll('.page-btn[data-page]').forEach((button) => {
        button.addEventListener('click', () => {
            const next = Number(button.dataset.page);
            if (next >= 1 && next <= totalPages && next !== page) {
                onChange(next);
            }
        });
    });
}

export function debounce(fn, delay = 260) {
    let timer = null;
    return (...args) => {
        window.clearTimeout(timer);
        timer = window.setTimeout(() => fn(...args), delay);
    };
}

export function drawBarChart(canvas, labels, values, options = {}) {
    if (!canvas) {
        return;
    }
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const width = Math.max(300, rect.width);
    const height = Math.max(180, rect.height);
    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);

    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, width, height);

    const padding = { top: 20, right: 16, bottom: 38, left: 50 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;
    const max = Math.max(1, ...values);
    const steps = 4;

    ctx.font = '12px "Microsoft YaHei", sans-serif';
    ctx.textBaseline = 'middle';

    for (let index = 0; index <= steps; index++) {
        const y = padding.top + chartHeight - (chartHeight * index) / steps;
        ctx.strokeStyle = '#edf1f8';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(width - padding.right, y);
        ctx.stroke();

        ctx.fillStyle = '#8a94a6';
        ctx.textAlign = 'right';
        ctx.fillText(String(Math.round((max * index) / steps)), padding.left - 8, y);
    }

    const slot = chartWidth / Math.max(1, values.length);
    const barWidth = Math.min(48, slot * 0.55);

    values.forEach((value, index) => {
        const barHeight = (value / max) * chartHeight;
        const x = padding.left + slot * index + (slot - barWidth) / 2;
        const y = padding.top + chartHeight - barHeight;

        const gradient = ctx.createLinearGradient(0, y, 0, padding.top + chartHeight);
        gradient.addColorStop(0, options.color || '#42a5f5');
        gradient.addColorStop(1, options.colorDark || '#1565c0');
        ctx.fillStyle = gradient;
        ctx.beginPath();
        if (barHeight > 0) {
            if (typeof ctx.roundRect === 'function') {
                ctx.roundRect(x, y, barWidth, barHeight, 6);
            } else {
                ctx.rect(x, y, barWidth, barHeight);
            }
            ctx.fill();
        }

        ctx.fillStyle = '#445066';
        ctx.textAlign = 'center';
        ctx.fillText(String(value), x + barWidth / 2, Math.max(y - 10, 10));

        ctx.fillStyle = '#7b8496';
        ctx.fillText(labels[index] ?? '', x + barWidth / 2, padding.top + chartHeight + 20);
    });
}

export function formValues(form) {
    return Object.fromEntries(new FormData(form).entries());
}
