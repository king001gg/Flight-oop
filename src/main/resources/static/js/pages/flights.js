import { apiGet } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    debounce,
    discountText,
    emptyHtml,
    escapeHtml,
    flightStatusBadge,
    loadingHtml,
    money,
    openModal,
    paginate,
    renderPagination,
    toast,
    closeModal
} from '../ui.js';

initLayout({ title: '航班查询', subtitle: '按城市、日期与余票筛选航班' });

const state = {
    flights: [],
    page: 1,
    pageSize: 8,
    selected: null
};

const tbody = $('#flightRows');
const pagination = $('#flightPagination');
const resultMeta = $('#resultMeta');
const filterForm = $('#flightFilterForm');
const flightNoInput = $('#flightNo');
const fromInput = $('#fromCity');
const toInput = $('#toCity');
const dateInput = $('#flightDate');
const onlyAvailable = $('#onlyAvailable');
const detailModal = $('#flightDetailModal');
const detailBody = $('#flightDetailBody');
const detailBookBtn = $('#detailBookBtn');

function escapeAttr(value) {
    return escapeHtml(value);
}

async function loadFlights() {
    tbody.innerHTML = `<tr><td colspan="9">${loadingHtml('正在查询航班…')}</td></tr>`;
    pagination.innerHTML = '';
    const params = {
        from: fromInput.value.trim(),
        to: toInput.value.trim(),
        date: dateInput.value,
        onlyAvailable: onlyAvailable.checked ? 'true' : ''
    };
    try {
        const result = await apiGet('/flights', params) || [];
        const flightNo = flightNoInput.value.trim().toLowerCase();
        state.flights = flightNo
            ? result.filter((flight) => flight.flightId.toLowerCase().includes(flightNo))
            : result;
        state.page = 1;
        render();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="9">${emptyHtml('查询失败', error.message)}</td></tr>`;
        toast(error.message, 'error');
    }
}

function render() {
    const page = paginate(state.flights, state.page, state.pageSize);
    state.page = page.page;
    resultMeta.textContent = `共 ${page.total} 个航班`;

    if (!page.total) {
        tbody.innerHTML = `<tr><td colspan="9">${emptyHtml('没有找到航班', '换个城市或日期试试')}</td></tr>`;
        pagination.innerHTML = '';
        return;
    }

    tbody.innerHTML = page.items.map((flight) => {
        const priceSub = Number(flight.discount) < 0.999
            ? `原价 ¥${money(flight.price)} · ${discountText(flight.discount)}`
            : '无折扣';
        return `
            <tr class="clickable" data-id="${escapeAttr(flight.flightId)}">
                <td>
                    <div class="cell-main">${escapeHtml(flight.flightId)}</div>
                    <div class="cell-sub">${escapeHtml(flight.airline)}</div>
                </td>
                <td>${escapeHtml(flight.date)}</td>
                <td>
                    <div class="cell-main">${escapeHtml(flight.takeOffTime)} – ${escapeHtml(flight.arrivalTime)}</div>
                    <div class="cell-sub">${flight.durationMinutes} 分钟</div>
                </td>
                <td>${escapeHtml(flight.route)}</td>
                <td>${escapeHtml(flight.planeModel)}</td>
                <td>
                    <div class="cell-main money">¥${money(flight.actualPrice)}</div>
                    <div class="cell-sub">${priceSub}</div>
                </td>
                <td>
                    <div class="status-cell" data-seats="${flight.remainingSeats}/${flight.totalSeats}"
                         title="余票 ${flight.remainingSeats} / ${flight.totalSeats}">
                        <span class="strong">${flight.remainingSeats}</span>
                        <span class="muted">/${flight.totalSeats}</span>
                    </div>
                </td>
                <td>${flightStatusBadge(flight.full, flight.remainingSeats)}</td>
                <td class="text-right">
                    <div class="actions" style="justify-content:flex-end">
                        <button class="btn btn-outline btn-sm" data-action="detail"
                                data-id="${escapeAttr(flight.flightId)}">详情</button>
                        ${flight.full
                            ? '<button class="btn btn-outline btn-sm" disabled>满仓</button>'
                            : `<button class="btn btn-primary btn-sm" data-action="book"
                                       data-id="${escapeAttr(flight.flightId)}">订票</button>`}
                    </div>
                </td>
            </tr>`;
    }).join('');

    renderPagination(pagination, page, (next) => {
        state.page = next;
        render();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
}

function findFlight(id) {
    return state.flights.find((flight) => flight.flightId === id);
}

function openDetail(flight) {
    if (!flight) {
        return;
    }
    state.selected = flight;
    const priceSub = Number(flight.discount) < 0.999
        ? `原价 ¥${money(flight.price)} · ${discountText(flight.discount)}`
        : '无折扣';
    detailBody.innerHTML = `
        <div class="flight-summary" style="margin-bottom:18px">
            <div class="meta">${escapeHtml(flight.airline)} · ${escapeHtml(flight.planeModel)}</div>
            <div class="route">${escapeHtml(flight.route)}</div>
            <div class="flight-times">
                <div class="time-block"><b>${escapeHtml(flight.takeOffTime)}</b><span>${escapeHtml(flight.date)}</span></div>
                <div class="time-line"></div>
                <div class="time-block"><b>${escapeHtml(flight.arrivalTime)}</b><span>${flight.durationMinutes} 分钟</span></div>
            </div>
            <div class="summary-chips">
                <span class="summary-chip">实付 ¥${money(flight.actualPrice)}</span>
                <span class="summary-chip">${priceSub}</span>
                <span class="summary-chip">余票 ${flight.remainingSeats}/${flight.totalSeats}</span>
                <span class="summary-chip">准点率 ${flight.onTimeRate}%</span>
            </div>
        </div>
        <dl class="detail-list">
            <dt>航班号</dt><dd>${escapeHtml(flight.flightId)}</dd>
            <dt>执飞日期</dt><dd>${escapeHtml(flight.date)}</dd>
            <dt>起降时刻</dt><dd>${escapeHtml(flight.takeOffTime)} – ${escapeHtml(flight.arrivalTime)}</dd>
            <dt>航线</dt><dd>${escapeHtml(flight.route)}</dd>
            <dt>机型 / 航司</dt><dd>${escapeHtml(flight.planeModel)} · ${escapeHtml(flight.airline)}</dd>
            <dt>票价 / 折扣</dt><dd>¥${money(flight.price)} · ${discountText(flight.discount)}</dd>
            <dt>余票</dt><dd>${flight.remainingSeats} / ${flight.totalSeats} ${flight.full ? '（满仓）' : ''}</dd>
            <dt>状态</dt><dd>${flightStatusBadge(flight.full, flight.remainingSeats)}</dd>
        </dl>`;
    detailBookBtn.disabled = flight.full;
    detailBookBtn.textContent = flight.full ? '已满仓' : '订票';
    openModal('flightDetailModal');
}

function goBooking(flight) {
    if (!flight || flight.full) {
        toast('该航班已满仓，请选择其他航班', 'warning');
        return;
    }
    location.href = `booking.html?flight=${encodeURIComponent(flight.flightId)}`;
}

filterForm.addEventListener('submit', (event) => {
    event.preventDefault();
    loadFlights();
});

flightNoInput.addEventListener('input', debounce(loadFlights, 420));
fromInput.addEventListener('input', debounce(loadFlights, 420));
toInput.addEventListener('input', debounce(loadFlights, 420));
dateInput.addEventListener('change', loadFlights);
onlyAvailable.addEventListener('change', loadFlights);

$('#clearFilters').addEventListener('click', () => {
    filterForm.reset();
    loadFlights();
});

tbody.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action]');
    if (button) {
        event.stopPropagation();
        const flight = findFlight(button.dataset.id);
        if (button.dataset.action === 'book') {
            goBooking(flight);
        } else {
            openDetail(flight);
        }
        return;
    }
    const row = event.target.closest('tr[data-id]');
    if (row) {
        openDetail(findFlight(row.dataset.id));
    }
});

tbody.addEventListener('dblclick', (event) => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
        goBooking(findFlight(row.dataset.id));
    }
});

tbody.addEventListener('mouseover', (event) => {
    const cell = event.target.closest('.status-cell');
    if (cell) {
        cell.title = `余票 ${cell.dataset.seats}`;
    }
});

detailBookBtn.addEventListener('click', () => {
    closeModal('flightDetailModal');
    goBooking(state.selected);
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadFlights);
} else {
    loadFlights();
}
