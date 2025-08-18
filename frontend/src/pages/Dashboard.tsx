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
      <div className="flex items-center gap-2">
        <label>Aggregation:</label>
        <select value={agg} onChange={e => setAgg(e.target.value as Aggregation)} className="border rounded px-2 py-1">
          <option>DAY</option>
          <option>MONTH</option>
          <option>QUARTER</option>
          <option>YEAR</option>
        </select>
      </div>

      {/* Quick Access Cards */}
      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg shadow-lg p-6 text-white">
          <h3 className="text-lg font-semibold mb-2">SKU Group Management</h3>
          <p className="text-blue-100 mb-4">Organize SKUs into logical groups for better analytics</p>
          <a 
            href="/sku-groups" 
            className="inline-flex items-center px-4 py-2 bg-white text-blue-600 rounded-md font-medium hover:bg-blue-50 transition-colors"
          >
            Manage Groups →
          </a>
        </div>
        
        <div className="bg-gradient-to-r from-green-500 to-green-600 rounded-lg shadow-lg p-6 text-white">
          <h3 className="text-lg font-semibold mb-2">Upload Data</h3>
          <p className="text-green-100 mb-4">Upload orders, payments, and SKU pricing data</p>
          <a 
            href="/upload" 
            className="inline-flex items-center px-4 py-2 bg-white text-green-600 rounded-md font-medium hover:bg-green-50 transition-colors"
          >
            Upload Data →
          </a>
        </div>
        
        <div className="bg-gradient-to-r from-purple-500 to-purple-600 rounded-lg shadow-lg p-6 text-white">
          <h3 className="text-lg font-semibold mb-2">Analytics Dashboard</h3>
          <p className="text-purple-100 mb-4">View comprehensive business insights and trends</p>
          <span className="inline-flex items-center px-4 py-2 bg-white text-purple-600 rounded-md font-medium">
            Current Page
          </span>
        </div>
        
                      <div className="bg-gradient-to-r from-red-500 to-red-600 rounded-lg shadow-lg p-6 text-white">
                <h3 className="text-lg font-semibold mb-2">Loss Analysis</h3>
                <p className="text-red-100 mb-4">Identify orders that resulted in losses despite successful delivery</p>
                <a 
                  href="/loss-analysis" 
                  className="inline-flex items-center px-4 py-2 bg-white text-red-600 rounded-md font-medium hover:bg-red-50 transition-colors"
                >
                  View Loss Analysis →
                </a>
              </div>
              
              <div className="bg-gradient-to-r from-purple-500 to-purple-600 rounded-lg shadow-lg p-6 text-white">
                <h3 className="text-lg font-semibold mb-2">Return Analysis</h3>
                <p className="text-purple-100 mb-4">Analyze returns and orders with negative settlement amounts</p>
                <a 
                  href="/return-analysis" 
                  className="inline-flex items-center px-4 py-2 bg-white text-purple-600 rounded-md font-medium hover:bg-purple-50 transition-colors"
                >
                  View Return Analysis →
                </a>
              </div>
              
              <div className="bg-gradient-to-r from-indigo-500 to-indigo-600 rounded-lg shadow-lg p-6 text-white">
                <h3 className="text-lg font-semibold mb-2">Return Tracking</h3>
                <p className="text-indigo-100 mb-4">Track return orders and manage receipt status</p>
                <a 
                  href="/return-tracking" 
                  className="inline-flex items-center px-4 py-2 bg-white text-indigo-600 rounded-md font-medium hover:bg-indigo-50 transition-colors"
                >
                  View Return Tracking →
                </a>
              </div>
      </div>

      {/* Month/Year selector and KPI cards */}
      <div className="flex items-center gap-4 mb-4">
        <label className="text-sm">Month:</label>
        <select value={month} onChange={e => setMonth(Number(e.target.value))} className="border rounded px-2 py-1">
          {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
            <option key={m} value={m}>{m.toString().padStart(2,'0')}</option>
          ))}
        </select>
        <label className="text-sm">Year:</label>
        <select value={year} onChange={e => setYear(Number(e.target.value))} className="border rounded px-2 py-1">
          {Array.from({ length: 6 }, (_, i) => now.getFullYear() - i).map(y => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>
      </div>

      <div className="grid md:grid-cols-5 gap-4 mb-8">
        <Kpi title="Total Revenue" value={`₹${Number(summary?.totalRevenue || 0).toLocaleString()}`}/>
        <Kpi title="Total Profit" value={`₹${Number(summary?.totalProfit || 0).toLocaleString()}`}/>
        <Kpi title="Total Orders" value={`${Number(summary?.totalOrders || 0).toLocaleString()}`}/>
        <Kpi title="Total Loss" value={`₹${Number(summary?.totalLoss || 0).toLocaleString()}`}/>
        <Kpi title="Net Income" value={`₹${Number(summary?.netIncome || 0).toLocaleString()}`}/>
      </div>

      {/* Detailed Loss Breakdown */}
      <div className="grid md:grid-cols-3 gap-4 mb-8">
        <Kpi title="Loss from Delivered Items" value={`₹${Number(comprehensiveLoss?.lossFromDelivered || 0).toLocaleString()}`}/>
        <Kpi title="Loss from Returns" value={`₹${Number(comprehensiveLoss?.lossFromReturns || 0).toLocaleString()}`}/>
        <Kpi title="Total Loss Orders" value={`${Number(comprehensiveLoss?.totalLossOrders || 0).toLocaleString()}`}/>
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
        <h2 className="text-2xl font-bold text-gray-900 mb-6">SKU Group Analytics</h2>
        <SkuGroupCharts />
      </div>
    </div>
  )
}

function ChartCard({ title, children }: { title: string, children: any }) {
  return (
    <div className="bg-white rounded shadow p-4">
      <div className="font-semibold mb-2">{title}</div>
      <div className="h-64">
        {children}
      </div>
    </div>
  )
}

function Kpi({ title, value }: { title: string, value: string }) {
  return (
    <div className="bg-white rounded shadow p-4">
      <div className="text-sm text-gray-500">{title}</div>
      <div className="text-2xl font-semibold mt-1">{value}</div>
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


