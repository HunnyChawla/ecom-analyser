import { useEffect, useState } from 'react'
import axios from 'axios'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts'
import SkuGroupCharts from '../components/SkuGroupCharts'

type Aggregation = 'DAY' | 'MONTH' | 'YEAR' | 'QUARTER'

type TimePoint = { period: string, value: number }

const api = axios.create({ baseURL: 'http://localhost:8080' })

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
  const end = new Date()
  const start = new Date(end.getTime() - 30*24*3600*1000)
  const startStr = start.toISOString().slice(0,10)
  const endStr = end.toISOString().slice(0,10)

  const orders = useTimeSeries('orders-by-time', agg, startStr, endStr)
  const payments = useTimeSeries('payments-by-time', agg, startStr, endStr)
  const profit = useTimeSeries('profit-trend', agg, startStr, endStr)
  const loss = useTimeSeries('loss-trend', agg, startStr, endStr)

  const [topOrdered, setTopOrdered] = useState<any[]>([])
  const [topProfit, setTopProfit] = useState<any[]>([])
  const [ordersByStatus, setOrdersByStatus] = useState<any[]>([])

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
  }, [startStr, endStr])

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
      <div className="grid md:grid-cols-3 gap-6 mb-8">
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


