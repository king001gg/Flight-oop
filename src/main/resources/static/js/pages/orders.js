import { apiGet, apiPost } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    closeModal,
    confirmAction,
    debounce,
    emptyHtml,
    escapeHtml,
    loadingHtml,
    money,
    openModal,
    paginate,
    renderPagination,
    statusBadge,
    toast
} from '../ui.js';

const user = initLayout({ title: '订单中心', subtitle: '查询订单、查看详情与退票' });

const state = {
    orders: [],
    page: 1,
    pageSize: 8,
    selected: null
};

const tbody = $('#orderRows');
const pagination = $('#orderPagination');
const meta = $('#orderMeta');
const keywordInput = $('#orderKeyword');
const statusSelect = $('#orderStatus');
const detailBody = $('#orderDetailBody');
const detailRefundBtn = $('#detailRefundBtn');

if (user.role === 'ADMIN') {
    $('#ordersTitle').textContent = '订单管理';
    $('#ordersSubtitle').textContent = '查看全部客户订单、筛选状态并处理退票';
} else {
    $('#ordersTitle').textContent = '我的订单';
    $('#ordersSubtitle').textContent = '查看自己的订单状态与退票操作';
}

async function loadOrders() {
    tbody.innerHTML = `<tr><td colspan="8">${loadingHtml('正在加载订单…')}</td></tr>`;
    pagination.innerHTML = '';
    try {
        state.orders = await apiGet('/orders', {
            keyword: keywordInput.value.trim(),
            status: statusSelect.value
        }) || [];
        state.page = 1;
        render();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8">${emptyHtml('加载订单失败', error.message)}</td></tr>`;
        toast(error.message, 'error');
    }
}

function render() {
    const page = paginate(state.orders, state.page, state.pageSize);
    state.page = page.page;
    meta.textContent = `共 ${page.total} 条`;

    if (!page.total) {
        tbody.innerHTML = `<tr><td colspan="8">${emptyHtml('还没有订单', user.role === 'ADMIN' ? '客户订票后会出现在这里' : '去航班查询页订一张票吧')}</td></tr>`;
        pagination.innerHTML = '';
        return;
    }

    tbody.innerHTML = page.items.map((order) => `
        <tr class="clickable" data-id="${escapeHtml(order.orderId)}">
            <td>
                <div class="cell-main nowrap">${escapeHtml(order.orderId)}</div>
            </td>
            <td>
                <div class="cell-main">${escapeHtml(order.flightId)} · ${escapeHtml(order.route || '')}</div>
                <div class="cell-sub">${escapeHtml(order.flightDate || '')}</div>
            </td>
            <td>
                <div class="cell-main">${escapeHtml(order.customerName)}</div>
                <div class="cell-sub">${escapeHtml(order.customerId)}</div>
            </td>
            <td class="text-center">${order.quantity}</td>
            <td class="text-right money">¥${money(order.amount)}</td>
            <td class="nowrap">${escapeHtml(order.orderTime)}</td>
            <td>${statusBadge(order.status)}</td>
            <td class="text-right">
                <div class="actions" style="justify-content:flex-end;flex-wrap:nowrap">
                    <button class="btn btn-outline btn-sm" data-action="detail"
                            data-id="${escapeHtml(order.orderId)}">详情</button>
                    ${order.status === 'BOOKED'
                        ? `<button class="btn btn-danger btn-sm" data-action="refund"
                                   data-id="${escapeHtml(order.orderId)}">退票</button>`
                        : '<button class="btn btn-outline btn-sm" disabled>已退票</button>'}
                </div>
            </td>
        </tr>`).join('');

    renderPagination(pagination, page, (next) => {
        state.page = next;
        render();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
}

function findOrder(id) {
    return state.orders.find((order) => order.orderId === id);
}

function openDetail(order) {
    if (!order) {
        return;
    }
    state.selected = order;
    detailBody.innerHTML = `
        <div class="meta-grid" style="margin-bottom:16px">
            <div class="meta-box"><span>订单号</span><b>${escapeHtml(order.orderId)}</b></div>
            <div class="meta-box"><span>订单状态</span><b>${statusBadge(order.status)}</b></div>
            <div class="meta-box"><span>客户</span><b>${escapeHtml(order.customerName)}</b></div>
            <div class="meta-box"><span>证件号</span><b>${escapeHtml(order.customerId)}</b></div>
            <div class="meta-box"><span>航班号</span><b>${escapeHtml(order.flightId)}</b></div>
            <div class="meta-box"><span>航线 / 日期</span><b>${escapeHtml(order.route || '')} · ${escapeHtml(order.flightDate || '')}</b></div>
            <div class="meta-box"><span>订票数量</span><b>${order.quantity} 张</b></div>
            <div class="meta-box"><span>单价 / 金额</span><b>¥${money(order.unitPrice)} / ¥${money(order.amount)}</b></div>
            <div class="meta-box"><span>下单时间</span><b>${escapeHtml(order.orderTime)}</b></div>
            <div class="meta-box"><span>退票时间</span><b>${escapeHtml(order.refundTime || '—')}</b></div>
        </div>
        <p class="muted small">订单金额使用下单时的折后单价计算；退票后座位会自动回补。</p>`;

    detailRefundBtn.classList.toggle('hidden', order.status !== 'BOOKED');
    detailRefundBtn.onclick = async () => {
        closeModal('orderDetailModal');
        await refundOrder(order);
    };
    openModal('orderDetailModal');
}

async function refundOrder(order) {
    const confirmed = await confirmAction({
        title: '确认退票',
        message: `订单号：${order.orderId}\n航班：${order.flightId} ${order.route || ''}\n客户：${order.customerName}\n金额：¥${money(order.amount)}\n\n退票后座位会自动回补，此操作不可撤销。`,
        confirmText: '确认退票',
        danger: true
    });
    if (!confirmed) {
        return;
    }
    try {
        await apiPost(`/orders/${encodeURIComponent(order.orderId)}/refund`);
        toast(`订单 ${order.orderId} 退票成功`, 'success');
        await loadOrders();
    } catch (error) {
        toast(error.message, 'error');
    }
}

$('#orderFilterForm').addEventListener('submit', (event) => {
    event.preventDefault();
    loadOrders();
});

statusSelect.addEventListener('change', loadOrders);
keywordInput.addEventListener('input', debounce(loadOrders, 450));

$('#resetOrders').addEventListener('click', () => {
    $('#orderFilterForm').reset();
    loadOrders();
});

tbody.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action]');
    if (button) {
        event.stopPropagation();
        const order = findOrder(button.dataset.id);
        if (button.dataset.action === 'refund') {
            refundOrder(order);
        } else {
            openDetail(order);
        }
        return;
    }
    const row = event.target.closest('tr[data-id]');
    if (row) {
        openDetail(findOrder(row.dataset.id));
    }
});

tbody.addEventListener('dblclick', (event) => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
        openDetail(findOrder(row.dataset.id));
    }
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadOrders);
} else {
    loadOrders();
}
