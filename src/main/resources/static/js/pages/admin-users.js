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
    loadingHtml,
    openModal,
    paginate,
    renderPagination,
    setButtonLoading,
    toast
} from '../ui.js';

initLayout({ adminOnly: true, title: '用户管理', subtitle: '维护登录账号、角色与客户关联' });

const state = {
    users: [],
    keyword: '',
    page: 1,
    pageSize: 8,
    editingUsername: null,
    resetUsername: null
};

const tbody = $('#userRows');
const pagination = $('#userPagination');
const form = $('#userForm');
const modalTitle = $('#userModalTitle');
const saveBtn = $('#saveUserBtn');
const passwordForm = $('#passwordForm');
const savePasswordBtn = $('#savePasswordBtn');

async function loadUsers() {
    tbody.innerHTML = `<tr><td colspan="4">${loadingHtml('正在加载用户…')}</td></tr>`;
    try {
        state.users = await apiGet('/admin/users') || [];
        state.page = 1;
        render();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="4">${emptyHtml('加载失败', error.message)}</td></tr>`;
        toast(error.message, 'error');
    }
}

function filteredUsers() {
    const keyword = state.keyword.toLowerCase();
    if (!keyword) {
        return state.users;
    }
    return state.users.filter((user) =>
        user.username.toLowerCase().includes(keyword)
        || user.displayName.toLowerCase().includes(keyword)
        || (user.customerId || '').toLowerCase().includes(keyword));
}

function roleBadge(user) {
    return user.role === 'ADMIN'
        ? '<span class="badge badge-warning">管理员</span>'
        : '<span class="badge badge-info">客户</span>';
}

function render() {
    const page = paginate(filteredUsers(), state.page, state.pageSize);
    state.page = page.page;

    if (!page.total) {
        tbody.innerHTML = `<tr><td colspan="4">${emptyHtml('没有匹配的用户', '换个关键词或新增用户')}</td></tr>`;
        pagination.innerHTML = '';
        return;
    }

    tbody.innerHTML = page.items.map((user) => `
        <tr data-id="${escapeHtml(user.username)}">
            <td>
                <span class="avatar-sm">${escapeHtml(initials(user.displayName))}</span>
                <span class="cell-main">${escapeHtml(user.displayName)}</span>
                <div class="cell-sub" style="margin-left:38px">${escapeHtml(user.username)}</div>
            </td>
            <td>${roleBadge(user)}</td>
            <td>${escapeHtml(user.customerId || '—')}</td>
            <td class="text-right">
                <div class="actions" style="justify-content:flex-end">
                    <button class="btn btn-outline btn-sm" data-action="edit"
                            data-id="${escapeHtml(user.username)}">编辑</button>
                    <button class="btn btn-outline btn-sm" data-action="password"
                            data-id="${escapeHtml(user.username)}">重置密码</button>
                    <button class="btn btn-danger btn-sm" data-action="delete"
                            data-id="${escapeHtml(user.username)}">删除</button>
                </div>
            </td>
        </tr>`).join('');

    renderPagination(pagination, page, (next) => {
        state.page = next;
        render();
    });
}

function findUser(username) {
    return state.users.find((user) => user.username === username);
}

function toggleCustomerField() {
    const isCustomer = $('#userRole').value === 'CUSTOMER';
    $('#customerIdField').classList.toggle('hidden', !isCustomer);
    $('#userCustomerId').required = isCustomer;
}

function openForm(user = null) {
    state.editingUsername = user?.username || null;
    form.reset();
    modalTitle.textContent = user ? '编辑用户' : '新增用户';
    saveBtn.textContent = user ? '保存修改' : '新增用户';
    const editing = Boolean(user);

    $('#userUsername').readOnly = editing;
    $('#passwordField').classList.toggle('hidden', editing);
    $('#userPassword').required = !editing;

    if (user) {
        $('#userUsername').value = user.username;
        $('#userDisplayName').value = user.displayName;
        $('#userRole').value = user.role;
        $('#userCustomerId').value = user.customerId || '';
    } else {
        $('#userRole').value = 'CUSTOMER';
    }
    toggleCustomerField();
    openModal('userFormModal');
}

async function submitForm(event) {
    event.preventDefault();
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    const role = $('#userRole').value;
    const displayName = $('#userDisplayName').value.trim();
    const customerId = $('#userCustomerId').value.trim();

    if (role === 'CUSTOMER' && !customerId) {
        toast('客户角色必须关联证件号', 'warning');
        return;
    }

    setButtonLoading(saveBtn, true, '保存中…');
    try {
        if (state.editingUsername) {
            await apiPut(`/admin/users/${encodeURIComponent(state.editingUsername)}`, {
                role,
                displayName,
                customerId: role === 'CUSTOMER' ? customerId : ''
            });
            toast('用户修改成功', 'success');
        } else {
            await apiPost('/admin/users', {
                username: $('#userUsername').value.trim(),
                password: $('#userPassword').value,
                role,
                displayName,
                customerId: role === 'CUSTOMER' ? customerId : ''
            });
            toast('用户新增成功', 'success');
        }
        closeModal('userFormModal');
        await loadUsers();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonLoading(saveBtn, false);
    }
}

function openPasswordModal(user) {
    state.resetUsername = user.username;
    passwordForm.reset();
    $('#passwordTarget').textContent = `为用户「${user.displayName}（${user.username}）」设置新密码`;
    openModal('passwordModal');
}

async function submitPassword(event) {
    event.preventDefault();
    if (!passwordForm.checkValidity()) {
        passwordForm.reportValidity();
        return;
    }
    setButtonLoading(savePasswordBtn, true, '重置中…');
    try {
        await apiPut(`/admin/users/${encodeURIComponent(state.resetUsername)}/password`, {
            password: $('#newPassword').value
        });
        toast('密码已重置，该用户需要重新登录', 'success');
        closeModal('passwordModal');
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonLoading(savePasswordBtn, false);
    }
}

async function deleteUser(user) {
    const confirmed = await confirmAction({
        title: '删除用户',
        message: `确定删除用户「${user.username}」吗？\n不能删除当前登录账号或最后一个管理员。`,
        confirmText: '删除',
        danger: true
    });
    if (!confirmed) {
        return;
    }
    try {
        await apiDelete(`/admin/users/${encodeURIComponent(user.username)}`);
        toast('用户已删除', 'success');
        await loadUsers();
    } catch (error) {
        toast(error.message, 'error');
    }
}

$('#addUserBtn').addEventListener('click', () => openForm(null));
$('#userRole').addEventListener('change', toggleCustomerField);
form.addEventListener('submit', submitForm);
passwordForm.addEventListener('submit', submitPassword);

$('#userFilter').addEventListener('submit', (event) => {
    event.preventDefault();
    state.keyword = $('#userKeyword').value.trim();
    state.page = 1;
    render();
});

$('#userKeyword').addEventListener('input', debounce(() => {
    state.keyword = $('#userKeyword').value.trim();
    state.page = 1;
    render();
}, 300));

$('#resetUserFilter').addEventListener('click', () => {
    $('#userFilter').reset();
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
    const user = findUser(button.dataset.id);
    if (!user) {
        return;
    }
    if (button.dataset.action === 'edit') {
        openForm(user);
    } else if (button.dataset.action === 'password') {
        openPasswordModal(user);
    } else {
        deleteUser(user);
    }
});

tbody.addEventListener('dblclick', (event) => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
        const user = findUser(row.dataset.id);
        if (user) {
            openForm(user);
        }
    }
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadUsers);
} else {
    loadUsers();
}
