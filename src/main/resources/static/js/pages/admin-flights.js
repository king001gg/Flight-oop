import { apiDelete, apiGet, apiPost, apiPut } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    closeModal,
    confirmAction,
    debounce,
    discountText,
    emptyHtml,
    escapeHtml,
    loadingHtml,
    money,
    openModal,
    paginate,
    renderPagination,
    setButtonLoading,
    toast
} from '../ui.js';

initLayout({ adminOnly: true, title: '航班管理', subtitle: '维护航班、时刻、票价与座位' });

const state = {
    flights: [],
    keyword: '',
    page: 1,
    pageSize: 8,
    editingId: null
};

const tbody = $('#adminFlightRows');
const pagination = $('#adminFlightPagination');
const modal = $('#flightFormModal');
const form = $('#flightForm');
const modalTitle = $('#flightModalTitle');
const saveBtn = $('#saveFlightBtn');

async function loadFlights() {
    tbody.innerHTML = `<tr><td colspan="8">${loadingHtml('正在加载航班…')}</td></tr>`;
    try {
        state.flights = await apiGet('/admin/flights') || [];
        state.page = 1;
        render();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8">${emptyHtml('加载失败', error.message)}</td></tr>`;
        toast(error.message, 'error');
    }
}

function filteredFlights() {
    const keyword = state.keyword.toLowerCase();
    if (!keyword) {
        return state.flights;
    }
    return state.flights.filter((flight) =>
        flight.flightId.toLowerCase().includes(keyword)
        || flight.startCity.toLowerCase().includes(keyword)
        || flight.endCity.toLowerCase().includes(keyword)
        || flight.airline.toLowerCase().includes(keyword)
        || flight.planeModel.toLowerCase().includes(keyword));
}

function render() {
    const page = paginate(filteredFlights(), state.page, state.pageSize);
    state.page = page.page;

    if (!page.total) {
        tbody.innerHTML = `<tr><td colspan="8">${emptyHtml('没有匹配的航班', '换个关键词或新增航班')}</td></tr>`;
        pagination.innerHTML = '';
        return;
    }

    tbody.innerHTML = page.items.map((flight) => `
        <tr data-id="${escapeHtml(flight.flightId)}">
            <td>
                <div class="cell-main">${escapeHtml(flight.flightId)}</div>
                <div class="cell-sub">${escapeHtml(flight.planeModel)}</div>
            </td>
            <td>
                <div class="cell-main">${escapeHtml(flight.date)}</div>
                <div class="cell-sub">${escapeHtml(flight.takeOffTime)} – ${escapeHtml(flight.arrivalTime)}</div>
            </td>
            <td>${escapeHtml(flight.route)}</td>
            <td>${escapeHtml(flight.planeModel)}</td>
            <td>
                <div class="cell-main money">¥${money(flight.actualPrice)}</div>
                <div class="cell-sub">¥${money(flight.price)} · ${discountText(flight.discount)}</div>
            </td>
            <td>${flight.remainingSeats} / ${flight.totalSeats}</td>
            <td>${escapeHtml(flight.airline)}</td>
            <td class="text-right">
                <div class="actions" style="justify-content:flex-end">
                    <button class="btn btn-outline btn-sm" data-action="edit"
                            data-id="${escapeHtml(flight.flightId)}">编辑</button>
                    <button class="btn btn-danger btn-sm" data-action="delete"
                            data-id="${escapeHtml(flight.flightId)}">删除</button>
                </div>
            </td>
        </tr>`).join('');

    renderPagination(pagination, page, (next) => {
        state.page = next;
        render();
    });
}

function openForm(flight = null) {
    state.editingId = flight?.flightId || null;
    form.reset();
    modalTitle.textContent = flight ? '编辑航班' : '新增航班';
    saveBtn.textContent = flight ? '保存修改' : '新增航班';

    if (flight) {
        $('#editingFlightId').value = flight.flightId;
        $('#flightId').value = flight.flightId;
        $('#date').value = flight.date;
        $('#takeOffTime').value = flight.takeOffTime;
        $('#durationMinutes').value = flight.durationMinutes;
        $('#startCity').value = flight.startCity;
        $('#endCity').value = flight.endCity;
        $('#planeModel').value = flight.planeModel;
        $('#price').value = flight.price;
        $('#discount').value = flight.discount;
        $('#totalSeats').value = flight.totalSeats;
        $('#airline').value = flight.airline;
        $('#onTimeRate').value = flight.onTimeRate;
    } else {
        $('#date').value = new Date().toISOString().slice(0, 10);
        $('#discount').value = '1';
        $('#durationMinutes').value = '120';
        $('#totalSeats').value = '180';
        $('#onTimeRate').value = '95';
    }
    openModal('flightFormModal');
}

function readForm() {
    return {
        flightId: $('#flightId').value.trim(),
        date: $('#date').value,
        takeOffTime: $('#takeOffTime').value,
        durationMinutes: Number($('#durationMinutes').value),
        startCity: $('#startCity').value.trim(),
        endCity: $('#endCity').value.trim(),
        planeModel: $('#planeModel').value.trim(),
        price: Number($('#price').value),
        discount: Number($('#discount').value),
        totalSeats: Number($('#totalSeats').value),
        airline: $('#airline').value.trim(),
        onTimeRate: Number($('#onTimeRate').value)
    };
}

async function submitForm(event) {
    event.preventDefault();
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    const payload = readForm();
    if (!payload.flightId || !payload.date || !payload.takeOffTime) {
        toast('请填写航班号、日期和起飞时间', 'warning');
        return;
    }
    setButtonLoading(saveBtn, true, '保存中…');
    try {
        if (state.editingId) {
            await apiPut(`/admin/flights/${encodeURIComponent(state.editingId)}`, payload);
            toast('航班修改成功', 'success');
        } else {
            await apiPost('/admin/flights', payload);
            toast('航班新增成功', 'success');
        }
        closeModal('flightFormModal');
        await loadFlights();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonLoading(saveBtn, false);
    }
}

async function deleteFlight(flight) {
    const confirmed = await confirmAction({
        title: '删除航班',
        message: `确定删除航班「${flight.flightId}」吗？\n存在订单记录的航班不能删除。`,
        confirmText: '删除',
        danger: true
    });
    if (!confirmed) {
        return;
    }
    try {
        await apiDelete(`/admin/flights/${encodeURIComponent(flight.flightId)}`);
        toast('航班已删除', 'success');
        await loadFlights();
    } catch (error) {
        toast(error.message, 'error');
    }
}

function findFlight(id) {
    return state.flights.find((flight) => flight.flightId === id);
}

$('#addFlightBtn').addEventListener('click', () => openForm(null));
form.addEventListener('submit', submitForm);

$('#flightAdminFilter').addEventListener('submit', (event) => {
    event.preventDefault();
    state.keyword = $('#flightKeyword').value.trim();
    state.page = 1;
    render();
});

$('#flightKeyword').addEventListener('input', debounce(() => {
    state.keyword = $('#flightKeyword').value.trim();
    state.page = 1;
    render();
}, 300));

$('#resetFlightFilter').addEventListener('click', () => {
    $('#flightAdminFilter').reset();
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
    const flight = findFlight(button.dataset.id);
    if (!flight) {
        return;
    }
    if (button.dataset.action === 'edit') {
        openForm(flight);
    } else {
        deleteFlight(flight);
    }
});

tbody.addEventListener('dblclick', (event) => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
        const flight = findFlight(row.dataset.id);
        if (flight) {
            openForm(flight);
        }
    }
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadFlights);
} else {
    loadFlights();
}
