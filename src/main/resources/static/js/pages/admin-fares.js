import { apiGet, apiPost, apiPut } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    confirmAction,
    emptyHtml,
    escapeHtml,
    loadingHtml,
    money,
    setButtonLoading,
    toast
} from '../ui.js';

initLayout({ adminOnly: true, title: '票价管理', subtitle: '按航班或航线维护原价与折扣' });

const state = {
    flights: []
};

const groupsContainer = $('#fareGroups');
const bulkForm = $('#bulkFareForm');

async function loadFlights() {
    groupsContainer.innerHTML = loadingHtml('正在加载票价…');
    try {
        state.flights = await apiGet('/admin/flights') || [];
        renderGroups();
    } catch (error) {
        groupsContainer.innerHTML = emptyHtml('加载票价失败', error.message);
        toast(error.message, 'error');
    }
}

function renderGroups() {
    if (!state.flights.length) {
        groupsContainer.innerHTML = emptyHtml('暂无航班', '请先到航班管理中添加航班');
        return;
    }

    const groups = new Map();
    for (const flight of state.flights) {
        if (!groups.has(flight.route)) {
            groups.set(flight.route, []);
        }
        groups.get(flight.route).push(flight);
    }

    groupsContainer.innerHTML = Array.from(groups.entries()).map(([route, flights]) => `
        <section class="route-group">
            <div class="route-group-head">
                <h3>${escapeHtml(route)}</h3>
                <span class="fare-hint">${flights.length} 个航班</span>
            </div>
            <div class="table-scroll">
                <table class="table">
                    <thead>
                    <tr>
                        <th>航班</th>
                        <th>日期</th>
                        <th>原价（元）</th>
                        <th>折扣（0~1）</th>
                        <th class="text-right">实付价</th>
                        <th class="text-right">操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${flights.map((flight) => `
                        <tr data-id="${escapeHtml(flight.flightId)}">
                            <td>
                                <div class="cell-main">${escapeHtml(flight.flightId)}</div>
                                <div class="cell-sub">${escapeHtml(flight.planeModel)} · ${escapeHtml(flight.airline)}</div>
                            </td>
                            <td>${escapeHtml(flight.date)}</td>
                            <td>
                                <input class="table-input fare-price" type="number" min="0" step="0.01"
                                       value="${flight.price}" data-id="${escapeHtml(flight.flightId)}">
                            </td>
                            <td>
                                <input class="table-input fare-discount" type="number" min="0.01" max="1" step="0.01"
                                       value="${flight.discount}" data-id="${escapeHtml(flight.flightId)}">
                            </td>
                            <td class="text-right money actual-price">¥${money(flight.actualPrice)}</td>
                            <td class="text-right">
                                <button class="btn btn-primary btn-sm" data-save
                                        data-id="${escapeHtml(flight.flightId)}">保存</button>
                            </td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>
        </section>`).join('');
}

function recalculateRow(row) {
    const price = Number(row.querySelector('.fare-price').value);
    const discount = Number(row.querySelector('.fare-discount').value);
    const cell = row.querySelector('.actual-price');
    cell.textContent = `¥${money((Number.isFinite(price) ? price : 0) * (Number.isFinite(discount) ? discount : 0))}`;
}

async function saveRow(button) {
    const row = button.closest('tr');
    const flightId = button.dataset.id;
    const price = Number(row.querySelector('.fare-price').value);
    const discount = Number(row.querySelector('.fare-discount').value);

    if (!Number.isFinite(price) || price < 0) {
        toast('原价必须是非负数字', 'warning');
        return;
    }
    if (!Number.isFinite(discount) || discount <= 0 || discount > 1) {
        toast('折扣必须在 0~1 之间', 'warning');
        return;
    }

    const confirmed = await confirmAction({
        title: '保存票价',
        message: `航班：${flightId}\n原价：¥${money(price)}\n折扣：${discount}\n实付价：¥${money(price * discount)}`,
        confirmText: '保存'
    });
    if (!confirmed) {
        return;
    }

    setButtonLoading(button, true, '保存中…');
    try {
        await apiPut(`/admin/flights/${encodeURIComponent(flightId)}/fare`, { price, discount });
        toast('票价已保存', 'success');
        await loadFlights();
    } catch (error) {
        toast(error.message, 'error');
        setButtonLoading(button, false);
    }
}

groupsContainer.addEventListener('input', (event) => {
    const input = event.target.closest('.fare-price, .fare-discount');
    if (input) {
        recalculateRow(input.closest('tr'));
    }
});

groupsContainer.addEventListener('change', (event) => {
    const input = event.target.closest('.fare-price, .fare-discount');
    if (input) {
        recalculateRow(input.closest('tr'));
    }
});

groupsContainer.addEventListener('click', (event) => {
    const button = event.target.closest('[data-save]');
    if (button) {
        saveRow(button);
    }
});

bulkForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!bulkForm.checkValidity()) {
        bulkForm.reportValidity();
        return;
    }
    const startCity = $('#bulkStartCity').value.trim();
    const endCity = $('#bulkEndCity').value.trim();
    const discount = Number($('#bulkDiscount').value);
    if (discount <= 0 || discount > 1) {
        toast('折扣必须在 0~1 之间', 'warning');
        return;
    }

    const confirmed = await confirmAction({
        title: '批量设置折扣',
        message: `航线：${startCity} → ${endCity}\n统一折扣：${discount}\n将修改该航线全部航班的折扣。`,
        confirmText: '确认设置'
    });
    if (!confirmed) {
        return;
    }

    const button = $('#bulkFareBtn');
    setButtonLoading(button, true, '设置中…');
    try {
        const result = await apiPost('/admin/fares/bulk', { startCity, endCity, discount });
        toast(`已更新 ${result.updatedFlights} 个航班的折扣`, 'success');
        await loadFlights();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        setButtonLoading(button, false);
    }
});

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadFlights);
} else {
    loadFlights();
}
