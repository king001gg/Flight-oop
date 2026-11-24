import { logout } from './auth.js';
import { requireAdmin, requireAuth } from './session.js';
import { escapeHtml, initials } from './ui.js';

const icons = {
    plane: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z"/></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.6-3.6"/></svg>',
    ticket: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"><path d="M4 5h16v4a2 2 0 0 0 0 6v4H4v-4a2 2 0 0 0 0-6V5z"/><path d="M13 5v14"/></svg>',
    list: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M8 6h12M8 12h12M8 18h12"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/></svg>',
    planeCog: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 15v-1.5l5-3V5.5a1.2 1.2 0 0 1 2.4 0v4.8l3.3 2"/><path d="M3 15l5-1.4v3.1l-1.2.9v1.1l2-.6 2 .6v-1.1l-1.2-.9v-3.9"/><circle cx="18" cy="15" r="2"/><path d="M18 11.8v1M18 17.2v1M14.8 15h1M20.2 15h1M15.7 12.7l.7.7M19.6 16.6l.7.7M20.3 12.7l-.7.7M16.4 16.6l-.7.7"/></svg>',
    users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3.5 20a5.5 5.5 0 0 1 11 0"/><circle cx="17" cy="9" r="2.4"/><path d="M16 14.8a4.4 4.4 0 0 1 4.5 4.2"/></svg>',
    tag: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13.2 13.2 20a2 2 0 0 1-2.8 0L4 13.6V4h9.6L20 10.4a2 2 0 0 1 0 2.8z"/><circle cx="8.5" cy="8.5" r="1.2"/></svg>',
    chart: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M4 20V6M4 20h16"/><path d="M8 16v-4M12 16V9M16 16v-6M20 16v-2"/></svg>',
    userCog: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3"/><path d="M3.5 20a5.5 5.5 0 0 1 10.2-2.9"/><circle cx="18" cy="16.5" r="2.5"/><path d="M18 12.5v1M18 19.5v1M14.5 14.5l.9.9M20.6 17.6l.9.9M21.5 14.5l-.9.9M15.4 17.6l-.9.9"/></svg>',
    logout: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"/><path d="M10 8l-4 4 4 4M6 12h10"/></svg>',
    menu: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>'
};

const commonNav = [
    { key: 'flights', href: 'flights.html', label: '航班查询', icon: 'search' },
    { key: 'booking', href: 'booking.html', label: '机票预订', icon: 'ticket' },
    { key: 'orders', href: 'orders.html', label: '我的订单', icon: 'list', adminLabel: '订单管理' }
];

const adminNav = [
    { key: 'admin-flights', href: 'admin-flights.html', label: '航班管理', icon: 'planeCog' },
    { key: 'admin-customers', href: 'admin-customers.html', label: '客户管理', icon: 'users' },
    { key: 'admin-fares', href: 'admin-fares.html', label: '票价管理', icon: 'tag' },
    { key: 'reports', href: 'reports.html', label: '报表打印', icon: 'chart' },
    { key: 'admin-users', href: 'admin-users.html', label: '用户管理', icon: 'userCog' }
];

function navHtml(items, currentPage) {
    return items.map((item) => `
        <a class="nav-item ${item.key === currentPage ? 'active' : ''}" href="${item.href}">
            ${icons[item.icon] || ''}
            <span>${escapeHtml(item.label)}</span>
        </a>`).join('');
}

export function initLayout({ adminOnly = false, title, subtitle } = {}) {
    const user = adminOnly ? requireAdmin() : requireAuth();
    const page = document.body.dataset.page || '';
    const pageTitle = title || document.body.dataset.title || document.title;
    const pageSubtitle = subtitle || document.body.dataset.subtitle || '';
    const isAdmin = user.role === 'ADMIN';

    const sidebar = document.getElementById('sidebar');
    const topbar = document.getElementById('topbar');
    if (!sidebar || !topbar) {
        throw new Error('页面缺少 sidebar/topbar 容器');
    }

    const businessItems = commonNav.map((item) => ({
        ...item,
        label: isAdmin && item.adminLabel ? item.adminLabel : item.label
    }));

    sidebar.innerHTML = `
        <div class="brand">
            <div class="brand-mark">${icons.plane}</div>
            <div>
                <div class="brand-name">航空订票</div>
                <div class="brand-sub">FLIGHT BOOKING</div>
            </div>
        </div>
        <nav class="nav">
            <div class="nav-section-title">业务</div>
            ${navHtml(businessItems, page)}
            ${isAdmin ? `
                <div class="nav-divider"></div>
                <div class="nav-section-title">管理</div>
                ${navHtml(adminNav, page)}
            ` : ''}
        </nav>
        <div class="sidebar-footer">
            <div class="user-avatar">${escapeHtml(initials(user.displayName))}</div>
            <div class="user-meta">
                <strong>${escapeHtml(user.displayName)}</strong>
                <span>${escapeHtml(user.roleText)}</span>
            </div>
            <button class="icon-btn" id="logoutBtn" title="退出登录" aria-label="退出登录"
                    style="color:#8fa6c6">${icons.logout}</button>
        </div>`;

    const dateText = new Date().toLocaleDateString('zh-CN', {
        year: 'numeric', month: 'long', day: 'numeric', weekday: 'short'
    });

    topbar.innerHTML = `
        <div class="topbar-left">
            <button class="menu-toggle" id="menuToggle" aria-label="打开菜单">${icons.menu}</button>
            <div class="topbar-title">
                <h1>${escapeHtml(pageTitle)}</h1>
                <p>${escapeHtml(pageSubtitle)}</p>
            </div>
        </div>
        <div class="topbar-right">
            <span class="topbar-date">${escapeHtml(dateText)}</span>
            <div class="user-chip">
                <span class="user-avatar">${escapeHtml(initials(user.displayName))}</span>
                <span class="user-name">${escapeHtml(user.displayName)}</span>
            </div>
        </div>`;

    const overlay = document.getElementById('sidebar-overlay');
    const toggle = document.getElementById('menuToggle');
    toggle?.addEventListener('click', () => document.body.classList.toggle('sidebar-open'));
    overlay?.addEventListener('click', () => document.body.classList.remove('sidebar-open'));
    sidebar.querySelectorAll('.nav-item').forEach((item) => {
        item.addEventListener('click', () => document.body.classList.remove('sidebar-open'));
    });
    document.getElementById('logoutBtn')?.addEventListener('click', () => logout());

    window.addEventListener('scroll', () => {
        topbar.classList.toggle('scrolled', window.scrollY > 4);
    }, { passive: true });

    return user;
}

export { icons };
