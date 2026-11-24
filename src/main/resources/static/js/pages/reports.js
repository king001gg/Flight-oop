import { apiGet } from '../api.js';
import { initLayout } from '../layout.js';
import {
    $,
    debounce,
    drawBarChart,
    emptyHtml,
    escapeHtml,
    firstDayOfMonthISO,
    lastDayOfMonthISO,
    loadingHtml,
    money,
    toast
} from '../ui.js';

initLayout({ adminOnly: true, title: '报表打印', subtitle: '航线时段统计与月度售票报表' });

const state = {
    monthLabels: [],
    monthValues: []
};

const chartCanvas = $('#monthlyChart');

async function loadSummary() {
    try {
        const summary = await apiGet('/stats/summary');
        $('#kpiFlights').textContent = summary.flightCount;
        $('#kpiOrders').textContent = summary.activeOrderCount;
        $('#kpiPassengers').textContent = summary.passengerCount;
        $('#kpiRevenue').textContent = `¥${money(summary.revenue)}`;
    } catch (error) {
        toast(error.message, 'error');
    }
}

async function loadMonthly() {
    const tbody = $('#monthlyRows');
    tbody.innerHTML = `<tr><td colspan="5">${loadingHtml('正在加载月度统计…')}</td></tr>`;
    try {
        const months = await apiGet('/stats/monthly') || [];
        if (!months.length) {
            tbody.innerHTML = `<tr><td colspan="5">${emptyHtml('暂无月度数据', '有订单后自动生成')}</td></tr>`;
            state.monthLabels = [];
            state.monthValues = [];
            drawChart();
            return;
        }

        tbody.innerHTML = months.map((month) => `
            <tr>
                <td class="cell-main">${escapeHtml(month.month)}</td>
                <td class="text-right">${month.soldTickets}</td>
                <td class="text-right">${month.refundedTickets}</td>
                <td class="text-right">${month.netTickets}</td>
                <td class="text-right money">¥${money(month.netRevenue)}</td>
            </tr>`).join('');

        state.monthLabels = months.map((month) => month.month.slice(5) + '月');
        state.monthValues = months.map((month) => month.netTickets);
        drawChart();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="5">${emptyHtml('加载失败', error.message)}</td></tr>`;
        toast(error.message, 'error');
    }
}

function drawChart() {
    drawBarChart(chartCanvas, state.monthLabels, state.monthValues);
}

async function runRouteStatistics(event) {
    if (event) {
        event.preventDefault();
    }
    const startCity = $('#statsStartCity').value.trim();
    const endCity = $('#statsEndCity').value.trim();
    const from = $('#statsFrom').value;
    const to = $('#statsTo').value;

    if (!from || !to) {
        toast('请选择开始日期和结束日期', 'warning');
        return;
    }
    if ((startCity && !endCity) || (!startCity && endCity)) {
        toast('出发城市和到达城市请同时填写，或都留空统计全部航线', 'warning');
        return;
    }
    if (from > to) {
        toast('开始日期不能晚于结束日期', 'warning');
        return;
    }

    try {
        const stats = await apiGet('/stats/route', {
            startCity,
            endCity,
            from,
            to
        });
        $('#routeFlightCount').textContent = `${stats.flightCount} 次`;
        $('#routeSeats').textContent = `${stats.totalSeats} 个`;
        $('#routePassengers').textContent = `${stats.passengers} 人`;
        $('#routeOccupancy').textContent = `${Number(stats.occupancyRate).toFixed(1)}%`;
        toast('统计完成', 'success', 1600);
    } catch (error) {
        toast(error.message, 'error');
    }
}

function initDates() {
    $('#statsFrom').value = firstDayOfMonthISO();
    $('#statsTo').value = lastDayOfMonthISO();
    $('#printDate').textContent = `统计日期：${new Date().toLocaleString('zh-CN')}`;
}

$('#routeStatsForm').addEventListener('submit', runRouteStatistics);
$('#printBtn').addEventListener('click', () => window.print());

window.addEventListener('resize', debounce(drawChart, 200));

async function init() {
    initDates();
    await Promise.allSettled([loadSummary(), loadMonthly(), runRouteStatistics(null)]);
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
