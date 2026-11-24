import { apiDelete, apiGet, apiPost, apiPut } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    closeModal,
    confirmAction,
    debounce,
    emptyHtml,
    escapeHtml,
    initials,
    levelBadge,
    loadingHtml,
    money,
    openModal,
    paginate,
    renderPagination,
    setButtonLoading,
    statusBadge,
    toast
} from '../ui.js';

initLayout({ adminOnly: true, title: '客户管理', subtitle: '维护客户资料与查看客户订单' });

const state = {
    customers: [],
    keyword: '',
    page: 1,
    pageSize: 8,
    editingId: null
};

const tbody = $('#customerRows');
const pagination = $('#customerPagination');
const form = $('#customerForm');
const modalTitle = $('#customerModalTitle');
const saveBtn = $('#saveCustomerBtn');

async function loadCustomers() {
    tbody.innerHTML = `<tr><td colspan="6">${loadingHtml('正在加载客户…')}</td></tr>`;
    try {
        state.customers = await apiGet('/admin/customers') || [];
        state.page = 1;
        render();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6">${emptyHtml('加载失败', error.message)}</td></tr>`;
        toast(error.message, 'error');
    }
}

function filteredCustomers() {
    const keyword = state.keyword.toLowerCase();
    if (!keyword) {
        return state.customers;
    }
    return state.customers.filter((customer) =>
        customer.customerId.toLowerCase().includes(keyword)
        || customer.name.toLowerCase().includes(keyword)
        || (customer.phone || '').toLowerCase().includes(keyword)
        || (customer.email || '').toLowerCase().includes(keyword));
}

function render() {
    const page = paginate(filteredCustomers(), state.page, state.pageSize);
    state.page = page.page;

    if (!page.total) {
        tbody.innerHTML = `<tr><td colspan="6">${emptyHtml('没有匹配的客户', '换个关键词或新增客户')}</td></tr>`;
        pagination.innerHTML = '';
        return;
    }

    tbody.innerHTML = page.items.map((customer) => `
        <tr data-id="${escapeHtml(customer.customerId)}">
            <td>
                <span class="avatar-sm">${escapeHtml(initials(customer.name))}</span>
                <span class="cell-main">${escapeHtml(customer.name)}</span>
                <div class="cell-sub" style="margin-left:38px">${escapeHtml(customer.customerId)}</div>
            </td>
            <td>${escapeHtml(customer.phone || '—')}</td>
            <td>${escapeHtml(customer.email || '—')}</td>
            <td>${levelBadge(customer.level, customer.levelText)}</td>
            <td>${escapeHtml(customer.createdAt)}</td>
            <td class="text-right">
                <div class="actions" style="justify-content:flex-end">
                    <button class="btn btn-outline btn-sm" data-action="orders"
                            data-id="${escapeHtml(customer.customerId)}">订单</button>
                    <button class="btn btn-outline btn-sm" data-action="edit"
                            data-id="${escapeHtml(customer.customerId)}">编辑</button>
                    <button class="btn btn-danger btn-sm" data-action="delete"
                            data-id="${escapeHtml(customer.customerId)}">删除</button>
                </div>
            </td>
        </tr>`).join('');

    renderPagination(pagination, page, (next) => {
        state.page = next;
        render();
    });
}

function findCustomer(id) {
    return state.customers.find((customer) => customer.customerId === id);
}

function openForm(customer = null) {
    state.editingId = customer?.customerId || null;
    form.reset();
    modalTitle.textContent = customer ? '编辑客户' : '新增客户';
    saveBtn.textContent = customer ? '保存修改' : '新增客户';
    $('#customerId').readOnly = Boolean(customer);

    if (customer) {
        $('#customerId').value = customer.customerId;
        $('#customerName').value = customer.name;
        $('#customerPhone').value = customer.phone || '';
        $('#customerEmail').value = customer.email || '';
        $('#customerLevel').value = customer.level || 'NORMAL';
    } else {
        $('#customerLevel').value = 'NORMAL';
    }
    openModal('customerFormModal');
}

async function submitForm(event) {
    event.preventDefault();
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    const payload = {
        customerId: $('#customerId').value.trim(),
        name: $('#customerName').value.trim(),
        phone: $('#customerPhone').value.trim(),
        email: $('#customerEmail').value.trim(),
        level: $('#customerLevel').value
    };

    setButtonLoading(saveBtn, true, '保存中…');
    try {
        if (state.editingId) {
            await apiPut(`/admin/customers/${encodeURIComponent(state.editingId)}`, payload);
            toast('客户资料修改成功', 'success');
        } else {
            await apiPost('/admin/customers', payload);
            toast('客户新增成功', 'success');
        }
        closeModal('customerFormModal');
        await loadCustomers();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonLoading(saveBtn, false);
    }
}

async function deleteCustomer(customer) {
    const confirmed = await confirmAction({
        title: '删除客户',
        message: `确定删除客户「${customer.name}」吗？\n存在订单的客户不能删除。`,
        confirmText: '删除',
        danger: true
    });
    if (!confirmed) {
        return;
    }
    try {
        await apiDelete(`/admin/customers/${encodeURIComponent(customer.customerId)}`);
        toast('客户已删除', 'success');
        await loadCustomers();
    } catch (error) {
        toast(error.message, 'error');
    }
}

async function showOrders(customer) {
    $('#customerOrdersTitle').textContent = `${customer.name} 的订单`;
    const body = $('#customerOrdersBody');
    body.innerHTML = loadingHtml('正在加载订单…');
    openModal('customerOrdersModal');
    try {
        const orders = await apiGet(`/admin/customers/${encodeURIComponent(customer.customerId)}/orders`) || [];
        if (!orders.length) {
            body.innerHTML = emptyHtml('该客户暂无订单', '客户订票后会显示在这里');
            return;
        }
        body.innerHTML = `
            <div class="table-wrap">
                <div class="table-scroll">
                    <table class="table">
                        <thead><tr>
                            <th>订单号</th><th>航班</th><th>航线</th><th>数量</th>
                            <th class="text-right">金额</th><th>下单时间</th><th>状态</th>
                        </tr></thead>
                        <tbody>
                        ${orders.map((order) => `
                            <tr>
                                <td>${escapeHtml(order.orderId)}</td>
                                <td>${escapeHtml(order.flightId)}</td>
                                <td>${escapeHtml(order.route || '')}</td>
                                <td>${order.quantity}</td>
                                <td class="text-right money">¥${money(order.amount)}</td>
                                <td>${escapeHtml(order.orderTime)}</td>
                                <td>${statusBadge(order.status)}</td>
                            </tr>`).join('')}
                        </tbody>
                    </table>
                </div>
            </div>`;
    } catch (error) {
        body.innerHTML = emptyHtml('加载订单失败', error.message);
    }
}

$('#addCustomerBtn').addEventListener('click', () => openForm(null));
form.addEventListener('submit', submitForm);

$('#customerFilter').addEventListener('submit', (event) => {
    event.preventDefault();
    state.keyword = $('#customerKeyword').value.trim();
    state.page = 1;
    render();
});

$('#customerKeyword').addEventListener('input', debounce(() => {
    state.keyword = $('#customerKeyword').value.trim();
    state.page = 1;
    render();
}, 300));

$('#resetCustomerFilter').addEventListener('click', () => {
    $('#customerFilter').reset();
    state.keyword = '';
    state.page = 1;
    render();
});

tbody.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action]');
    if (!button) {
        return;
    }
    event.stopPropagation();
    const customer = findCustomer(button.dataset.id);
    if (!customer) {
        return;
    }
    if (button.dataset.action === 'edit') {
        openForm(customer);
    } else if (button.dataset.action === 'delete') {
        deleteCustomer(customer);
    } else {
        showOrders(customer);
    }
});

tbody.addEventListener('dblclick', (event) => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
        const customer = findCustomer(row.dataset.id);
        if (customer) {
            openForm(customer);
        }
    }
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadCustomers);
} else {
    loadCustomers();
}
