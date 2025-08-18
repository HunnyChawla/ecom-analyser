import { useEffect, useState } from 'react'
import axios from 'axios'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts'
import SkuGroupCharts from '../components/SkuGroupCharts'

type Aggregation = 'DAY' | 'MONTH' | 'YEAR' | 'QUARTER'

type TimePoint = { period: string, value: number }

const api = axios.create({ baseURL: 'http://localhost:8080' })

// Helper function to get proper month boundaries
function getMonthBoundaries(year: number, month: number): { start: Date, end: Date } {
  // month is 1-12, but JavaScript Date constructor expects 0-11
  const start = new Date(year, month - 1, 1)
  const end = new Date(year, month, 0)  // day 0 of next month = last day of current month
  
  // Validate that we got the correct dates
  console.log(`getMonthBoundaries(${year}, ${month}):`)
  console.log(`  Start: ${start.toDateString()} (should be day 1 of month ${month})`)
  console.log(`  End: ${end.toDateString()} (should be last day of month ${month})`)
  
  return { start, end }
}

function useTimeSeries(endpoint: string, agg: Aggregation, start: string, end: string) {
  const [data, setData] = useState<TimePoint[]>([])
  useEffect(() => {
    api.get(`/api/analytics/${endpoint}`, { params: { start, end, agg }})
      .then(r => setData(r.data.data.map((p: any) => ({ period: p.period, value: Number(p.value) }))))
      .catch(() => setData([]))
  }, [endpoint, agg, start, end])
  return data
}

export default function Dashboard() {
  const [agg, setAgg] = useState<Aggregation>('DAY')
  const now = new Date()
  const [year, setYear] = useState<number>(now.getFullYear())
  const [month, setMonth] = useState<number>(now.getMonth() + 1) // 1-12
  // Use helper function to get proper month boundaries
  // Expected behavior: July 2025 should give dates 2025-07-01 to 2025-07-31
  const { start, end } = getMonthBoundaries(year, month)
  
  // Debug logging to verify date ranges
  console.log(`Selected month: ${month}, year: ${year}`)
  console.log(`Start date: ${start.toDateString()}`)
  console.log(`End date: ${end.toDateString()}`)
  
  // Use local date formatting to avoid timezone issues
  const startStr = start.toLocaleDateString('en-CA')  // Returns YYYY-MM-DD format
  const endStr = end.toLocaleDateString('en-CA')      // Returns YYYY-MM-DD format
  
  console.log(`Final date range: ${startStr} to ${endStr}`)
  console.log(`Expected for month ${month}: ${year}-${month.toString().padStart(2, '0')}-01 to ${year}-${month.toString().padStart(2, '0')}-${new Date(year, month, 0).getDate()}`)

  const orders = useTimeSeries('orders-by-time', agg, startStr, endStr)
  const payments = useTimeSeries('payments-by-time', agg, startStr, endStr)
  const profit = useTimeSeries('profit-trend', agg, startStr, endStr)
  const loss = useTimeSeries('loss-trend', agg, startStr, endStr)

  const [topOrdered, setTopOrdered] = useState<any[]>([])
  const [topProfit, setTopProfit] = useState<any[]>([])
  const [ordersByStatus, setOrdersByStatus] = useState<any[]>([])
  const [summary, setSummary] = useState<any | null>(null)
  const [comprehensiveLoss, setComprehensiveLoss] = useState<any | null>(null)

  useEffect(() => {
    api.get('/api/analytics/top-ordered', { params: { start: startStr, end: endStr, limit: 10 }})
      .then(r => setTopOrdered(r.data))
      .catch(() => setTopOrdered([]))
    api.get('/api/analytics/top-profitable', { params: { start: startStr, end: endStr, limit: 10 }})
      .then(r => setTopProfit(r.data))
      .catch(() => setTopProfit([]))
    api.get('/api/analytics/orders-by-status', { params: { start: startStr, end: endStr }})
      .then(r => setOrdersByStatus(r.data))
      .catch(() => setOrdersByStatus([]))
    api.get('/api/analytics/monthly-summary', { params: { year, month }})
      .then(r => setSummary(r.data))
      .catch(() => setSummary(null))
    api.get('/api/analytics/comprehensive-loss-metrics', { params: { start: startStr, end: endStr }})
      .then(r => setComprehensiveLoss(r.data))
      .catch(() => setComprehensiveLoss(null))
  }, [startStr, endStr, year, month])

  return (
    <div className="space-y-6">

      {/* Month/Year selector and KPI cards */}
      <div className="bg-white rounded-xl shadow-lg p-6 mb-8 border border-gray-100">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-800">📅 Select Time Period</h2>
          <div className="text-sm text-gray-500">Dashboard will update automatically</div>
        </div>
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-3">
            <label className="text-sm font-medium text-gray-700">Month:</label>
            <select 
              value={month} 
              onChange={e => setMonth(Number(e.target.value))} 
              className="border border-gray-300 rounded-lg px-4 py-2 bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                <option key={m} value={m}>{m.toString().padStart(2,'0')}</option>
              ))}
            </select>
          </div>
          <div className="flex items-center gap-3">
            <label className="text-sm font-medium text-gray-700">Year:</label>
            <select 
              value={year} 
              onChange={e => setYear(Number(e.target.value))} 
              className="border border-gray-300 rounded-lg px-4 py-2 bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              {Array.from({ length: 6 }, (_, i) => now.getFullYear() - i).map(y => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Main KPI Cards */}
      <div className="grid md:grid-cols-5 gap-6 mb-8">
        <Kpi 
          title="Total Revenue" 
          value={`₹${Number(summary?.totalRevenue || 0).toLocaleString()}`}
          icon="💰"
          gradient="from-green-500 to-green-600"
          textColor="text-green-50"
        />
        <Kpi 
          title="Total Profit" 
          value={`₹${Number(summary?.totalProfit || 0).toLocaleString()}`}
          icon="📈"
          gradient="from-blue-500 to-blue-600"
          textColor="text-blue-50"
        />
        <Kpi 
          title="Total Orders" 
          value={`${Number(summary?.totalOrders || 0).toLocaleString()}`}
          icon="📦"
          gradient="from-purple-500 to-purple-600"
          textColor="text-purple-50"
        />
        <Kpi 
          title="Total Loss" 
          value={`₹${Number(summary?.totalLoss || 0).toLocaleString()}`}
          icon="📉"
          gradient="from-red-500 to-red-600"
          textColor="text-red-50"
        />
        <Kpi 
          title="Net Income" 
          value={`₹${Number(summary?.netIncome || 0).toLocaleString()}`}
          icon="💎"
          gradient="from-indigo-500 to-indigo-600"
          textColor="text-indigo-50"
        />
      </div>

      {/* Detailed Loss Breakdown */}
      <div className="grid md:grid-cols-3 gap-6 mb-8">
        <Kpi 
          title="Loss from Delivered Items" 
          value={`₹${Number(comprehensiveLoss?.lossFromDelivered || 0).toLocaleString()}`}
          icon="🚚"
          gradient="from-orange-500 to-orange-600"
          textColor="text-orange-50"
        />
        <Kpi 
          title="Loss from Returns" 
          value={`₹${Number(comprehensiveLoss?.lossFromReturns || 0).toLocaleString()}`}
          icon="↩️"
          gradient="from-pink-500 to-pink-600"
          textColor="text-pink-50"
        />
        <Kpi 
          title="Total Loss Orders" 
          value={`${Number(comprehensiveLoss?.totalLossOrders || 0).toLocaleString()}`}
          icon="⚠️"
          gradient="from-yellow-500 to-yellow-600"
          textColor="text-yellow-50"
        />
      </div>

      {/* Chart Aggregation Selector */}
      <div className="bg-white rounded-xl shadow-lg p-4 mb-6 border border-gray-100">
        <div className="flex items-center gap-4">
          <label className="text-sm font-medium text-gray-700">📊 Chart Aggregation:</label>
          <select 
            value={agg} 
            onChange={e => setAgg(e.target.value as Aggregation)} 
            className="border border-gray-300 rounded-lg px-4 py-2 bg-white text-gray-700 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option>DAY</option>
            <option>MONTH</option>
            <option>QUARTER</option>
            <option>YEAR</option>
          </select>
          <div className="text-sm text-gray-500">Choose how to group chart data</div>
        </div>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <ChartCard title="Orders by Timeframe">
          <LineArea data={orders} />
        </ChartCard>
        <ChartCard title="Payments by Timeframe">
          <LineArea data={payments} />
        </ChartCard>
        <ChartCard title="Profit Trends">
          <LineArea data={profit} />
        </ChartCard>
        <ChartCard title="Loss Trends">
          <LineArea data={loss} />
        </ChartCard>
        <ChartCard title="Top Ordered Items">
          <Bars data={topOrdered.map(d => ({ name: d.sku, value: Number(d.quantity) }))} />
        </ChartCard>
        <ChartCard title="Top Profitable SKUs">
          <Bars data={topProfit.map(d => ({ name: d.sku, value: Number(d.profit) }))} />
        </ChartCard>
        <ChartCard title="Orders by Status">
          <Bars data={ordersByStatus.map(d => ({ name: d.status, value: Number(d.count) }))} />
        </ChartCard>
      </div>

      {/* SKU Group Analytics Section */}
      <div className="mt-12">
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6 border border-gray-100">
          <h2 className="text-2xl font-bold text-gray-800 mb-2">📊 SKU Group Analytics</h2>
          <p className="text-gray-600">Performance metrics grouped by SKU categories</p>
        </div>
        <SkuGroupCharts />
      </div>
    </div>
  )
}

function ChartCard({ title, children }: { title: string, children: any }) {
  return (
    <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 border border-gray-100">
      <div className="font-bold text-lg text-gray-800 mb-4 border-b border-gray-200 pb-2">{title}</div>
      <div className="h-64">
        {children}
      </div>
    </div>
  )
}

function Kpi({ title, value, icon, gradient, textColor }: { 
  title: string, 
  value: string, 
  icon: string, 
  gradient: string, 
  textColor: string 
}) {
  return (
    <div className={`bg-gradient-to-r ${gradient} rounded-lg shadow-lg p-6 text-white hover:shadow-xl transition-all duration-300 transform hover:scale-105`}>
      <div className="flex items-center justify-between mb-4">
        <div className="text-4xl">{icon}</div>
        <div className="text-right">
          <div className={`text-sm ${textColor} opacity-90 font-medium`}>{title}</div>
        </div>
      </div>
      <div className={`text-3xl font-bold ${textColor}`}>{value}</div>
      <div className="mt-2 text-xs opacity-75">Updated just now</div>
    </div>
  )
}

function LineArea({ data }: { data: TimePoint[] }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data}>
        <XAxis dataKey="period" tick={{ fontSize: 12 }}/>
        <YAxis tick={{ fontSize: 12 }}/>
        <Tooltip />
        <Line type="monotone" dataKey="value" stroke="#2563eb" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  )
}

function Bars({ data }: { data: { name: string, value: number }[] }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data}>
        <XAxis dataKey="name" tick={{ fontSize: 12 }} interval={0} angle={-20} textAnchor="end" height={60}/>
        <YAxis tick={{ fontSize: 12 }}/>
        <Tooltip />
        <Bar dataKey="value" fill="#10b981" />
      </BarChart>
    </ResponsiveContainer>
  )
}


