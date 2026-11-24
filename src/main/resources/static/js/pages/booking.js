import { apiGet, apiPost } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    confirmAction,
    discountText,
    emptyHtml,
    escapeHtml,
    money,
    openModal,
    closeModal,
    setButtonLoading,
    toast
} from '../ui.js';

const user = initLayout({ title: '机票预订', subtitle: '确认航班、填写乘客信息并提交订单' });

const state = {
    flights: [],
    selected: null,
    profile: null,
    adminCustomer: null
};

const flightSelect = $('#flightSelect');
const flightSummary = $('#flightSummary');
const customerSelf = $('#customerSelf');
const adminBlock = $('#adminCustomerBlock');
const adminCustomerId = $('#adminCustomerId');
const adminCustomerResult = $('#adminCustomerResult');
const quantityInput = $('#quantity');
const unitPrice = $('#unitPrice');
const qtyText = $('#qtyText');
const totalPrice = $('#totalPrice');
const seatHint = $('#seatHint');
const bookingHint = $('#bookingHint');
const bookBtn = $('#bookBtn');

async function loadFlights() {
    flightSelect.innerHTML = '<option value="">加载中…</option>';
    try {
        state.flights = await apiGet('/flights') || [];
        if (!state.flights.length) {
            flightSelect.innerHTML = '<option value="">暂无可选航班</option>';
            flightSummary.innerHTML = emptyHtml('暂无航班', '请先到航班管理中添加航班');
            return;
        }

        const requested = new URLSearchParams(location.search).get('flight');
        const preferred = state.flights.find((flight) => flight.flightId === requested);
        const fallback = state.flights.find((flight) => !flight.full) || state.flights[0];

        flightSelect.innerHTML = state.flights.map((flight) => `
            <option value="${escapeHtml(flight.flightId)}">
                ${escapeHtml(flight.flightId)} · ${escapeHtml(flight.date)} ${escapeHtml(flight.takeOffTime)}
                · ${escapeHtml(flight.route)} · ${flight.full ? '满仓' : `余票 ${flight.remainingSeats}`}
            </option>`).join('');
        flightSelect.value = (preferred || fallback).flightId;
        renderSelectedFlight();
    } catch (error) {
        flightSelect.innerHTML = '<option value="">加载失败</option>';
        flightSummary.innerHTML = emptyHtml('加载航班失败', error.message);
        toast(error.message, 'error');
    }
}

async function loadProfile() {
    if (user.role === 'ADMIN') {
        adminBlock.classList.remove('hidden');
        customerSelf.classList.add('hidden');
        bookingHint.textContent = '管理员订票需要先按证件号查询客户';
        return;
    }
    try {
        state.profile = await apiGet('/customers/me');
        customerSelf.innerHTML = `
            <div class="meta-box"><span>客户姓名</span><b>${escapeHtml(state.profile.name)}</b></div>
            <div class="meta-box"><span>证件号</span><b>${escapeHtml(state.profile.customerId)}</b></div>`;
        customerSelf.classList.remove('hidden');
        adminBlock.classList.add('hidden');
        bookingHint.textContent = '订单将使用当前登录客户的资料生成';
    } catch (error) {
        customerSelf.innerHTML = `<div class="meta-box"><span>客户资料</span><b>${escapeHtml(error.message)}</b></div>`;
        toast(error.message, 'error');
    }
}

function findSelected() {
    return state.flights.find((flight) => flight.flightId === flightSelect.value) || null;
}

function renderSelectedFlight() {
    const flight = findSelected();
    state.selected = flight;
    if (!flight) {
        flightSummary.innerHTML = emptyHtml('请选择航班', '从上方下拉框选择一个航班');
        return;
    }

    const priceSub = Number(flight.discount) < 0.999
        ? `原价 ¥${money(flight.price)} · ${discountText(flight.discount)}`
        : '无折扣';
    const seatPercent = Math.max(0, Math.min(100,
        ((flight.totalSeats - flight.remainingSeats) / Math.max(1, flight.totalSeats)) * 100));

    flightSummary.innerHTML = `
        <div class="flight-summary">
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
            <div class="seat-bar" style="margin-top:18px;background:rgba(255,255,255,.22)">
                <div class="seat-fill ${flight.full ? 'full' : ''}" style="width:${seatPercent}%"></div>
            </div>
            <div class="small" style="color:rgba(255,255,255,.7);margin-top:6px">
                已订 ${flight.totalSeats - flight.remainingSeats} / ${flight.totalSeats} 座${flight.full ? ' · 已满仓' : ''}
            </div>
        </div>
        <div class="card card-pad mt-2">
            <div class="section-title">航班说明</div>
            <p class="muted small">
                ${flight.full
                    ? '该航班已满仓。提交订票后系统会推荐同航线、余票足够的替代航班。'
                    : '提交订单后可在「我的订单」或「订单管理」中查看、退票。'}
            </p>
        </div>`;
    updatePrice();
}

function updatePrice() {
    const flight = state.selected;
    const quantity = Math.max(1, Number(quantityInput.value) || 1);
    quantityInput.value = quantity;
    const price = flight ? Number(flight.actualPrice) : 0;
    unitPrice.textContent = `¥${money(price)}`;
    qtyText.textContent = `${quantity} 张`;
    totalPrice.textContent = `¥${money(price * quantity)}`;

    if (!flight) {
        seatHint.textContent = '';
        bookBtn.disabled = true;
        return;
    }
    bookBtn.disabled = false;
    if (flight.full) {
        seatHint.textContent = '该航班已满仓，提交后会推荐同航线的可替代航班。';
        seatHint.className = 'small';
    } else if (quantity > flight.remainingSeats) {
        seatHint.textContent = `余票不足：当前剩余 ${flight.remainingSeats} 张，提交后会推荐替代航班。`;
        seatHint.className = 'small';
    } else {
        seatHint.textContent = `余票充足，当前剩余 ${flight.remainingSeats} 张。`;
        seatHint.className = 'small muted';
    }
    seatHint.style.color = (flight.full || quantity > flight.remainingSeats) ? 'var(--warning)' : '';
}

async function lookupCustomer() {
    const keyword = adminCustomerId.value.trim();
    if (!keyword) {
        toast('请输入客户证件号', 'warning');
        return;
    }
    try {
        const customers = await apiGet('/admin/customers', { keyword }) || [];
        const customer = customers.find((item) => item.customerId === keyword) || customers[0];
        if (!customer) {
            state.adminCustomer = null;
            adminCustomerResult.innerHTML = '<span>客户信息</span><b>未找到该客户</b>';
            toast('未找到该客户，请先在客户管理中添加', 'warning');
            return;
        }
        state.adminCustomer = customer;
        adminCustomerId.value = customer.customerId;
        adminCustomerResult.innerHTML = `
            <span>客户信息</span>
            <b>${escapeHtml(customer.name)} · ${escapeHtml(customer.customerId)} · ${escapeHtml(customer.phone || '无电话')}</b>`;
        toast('客户信息已加载', 'success', 1600);
    } catch (error) {
        toast(error.message, 'error');
    }
}

function showAlternatives(error) {
    const alternatives = error.data?.alternatives || [];
    const body = $('#alternativesBody');
    if (!alternatives.length) {
        body.innerHTML = `<p class="muted">${escapeHtml(error.message)}</p>
            <p class="muted small">同航线暂时没有其他可订航班。</p>`;
        openModal('alternativesModal');
        return;
    }
    body.innerHTML = `
        <p class="muted">${escapeHtml(error.message)}</p>
        <p class="small muted">请选择同航线的可替代航班，选中后会自动回到表单：</p>
        <div class="alternative-list">
            ${alternatives.map((flight) => `
                <div class="alternative-item">
                    <div>
                        <div class="flight-no">${escapeHtml(flight.flightId)}</div>
                        <div class="alt-meta">${escapeHtml(flight.date)} ${escapeHtml(flight.takeOffTime)}
                            · ${escapeHtml(flight.route)} · 余票 ${flight.remainingSeats}
                            · 实付 ¥${money(flight.actualPrice)}</div>
                    </div>
                    <button class="btn btn-primary btn-sm" data-alt="${escapeHtml(flight.flightId)}">选择</button>
                </div>`).join('')}
        </div>`;
    openModal('alternativesModal');
}

async function submitBooking(event) {
    event.preventDefault();
    const flight = state.selected;
    if (!flight) {
        toast('请选择航班', 'warning');
        return;
    }
    const quantity = Math.max(1, Number(quantityInput.value) || 1);
    if (!Number.isInteger(quantity) || quantity <= 0) {
        toast('订票数量必须是正整数', 'warning');
        return;
    }
    if (user.role === 'ADMIN' && !state.adminCustomer) {
        toast('管理员订票前必须先查询客户', 'warning');
        return;
    }

    const customerName = user.role === 'ADMIN'
        ? state.adminCustomer?.name
        : state.profile?.name;
    if (!customerName) {
        toast('客户资料未加载，请刷新页面重试', 'error');
        return;
    }

    const confirmed = await confirmAction({
        title: '确认订票',
        message: `航班：${flight.flightId} ${flight.route}\n日期：${flight.date} ${flight.takeOffTime}\n客户：${customerName}\n数量：${quantity} 张\n总金额：¥${money(flight.actualPrice * quantity)}`,
        confirmText: '确认提交',
        danger: false
    });
    if (!confirmed) {
        return;
    }

    const payload = {
        flightId: flight.flightId,
        quantity
    };
    if (user.role === 'ADMIN') {
        payload.customerId = state.adminCustomer.customerId;
    }

    setButtonLoading(bookBtn, true, '提交中…');
    try {
        await apiPost('/orders', payload);
        toast('订票成功，正在跳转到订单页…', 'success', 1600);
        window.setTimeout(() => location.href = 'orders.html', 800);
    } catch (error) {
        if (error.status === 409) {
            showAlternatives(error);
        } else {
            toast(error.message, 'error');
        }
        setButtonLoading(bookBtn, false);
    }
}

flightSelect.addEventListener('change', renderSelectedFlight);
quantityInput.addEventListener('input', updatePrice);
quantityInput.addEventListener('change', updatePrice);
$('#lookupCustomer').addEventListener('click', lookupCustomer);
adminCustomerId.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
        event.preventDefault();
        lookupCustomer();
    }
});
$('#refreshFlights').addEventListener('click', loadFlights);
$('#bookingForm').addEventListener('submit', submitBooking);

$('#alternativesBody').addEventListener('click', (event) => {
    const button = event.target.closest('[data-alt]');
    if (!button) {
        return;
    }
    flightSelect.value = button.dataset.alt;
    closeModal('alternativesModal');
    renderSelectedFlight();
    toast('已切换替代航班，请确认后再次提交', 'info');
});

async function init() {
    await loadFlights();
    await loadProfile();
    updatePrice();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
