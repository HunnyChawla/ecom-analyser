import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const api = axios.create({ baseURL: 'http://localhost:8080' });

interface GroupAnalytics {
  groupName: string;
  orderCount: number;
  totalQuantity: number;
  totalRevenue: number;
  totalProfit: number;
}

interface RevenueContribution {
  groupName: string;
  revenue: number;
}

interface ProfitComparison {
  groupName: string;
  orderCount: number;
  totalQuantity: number;
  totalRevenue: number;
  totalProfit: number;
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D', '#FFC658', '#FF6B6B', '#4ECDC4', '#45B7D1'];

const SkuGroupCharts: React.FC = () => {
  const [topPerformingGroups, setTopPerformingGroups] = useState<GroupAnalytics[]>([]);
  const [revenueContribution, setRevenueContribution] = useState<RevenueContribution[]>([]);
  const [profitComparison, setProfitComparison] = useState<ProfitComparison[]>([]);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState({
    start: new Date(new Date().getFullYear(), 0, 1).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    fetchGroupAnalytics();
  }, [dateRange]);

  const fetchGroupAnalytics = async () => {
    try {
      setLoading(true);
      const [topPerforming, revenue, profit] = await Promise.all([
        api.get(`/api/sku-groups/analytics/top-performing?start=${dateRange.start}&end=${dateRange.end}`),
        api.get(`/api/sku-groups/analytics/revenue-contribution?start=${dateRange.start}&end=${dateRange.end}`),
        api.get(`/api/sku-groups/analytics/profit-comparison?start=${dateRange.start}&end=${dateRange.end}`)
      ]);
      setTopPerformingGroups(Array.isArray(topPerforming.data) ? topPerforming.data : []);
      setRevenueContribution(Array.isArray(revenue.data) ? revenue.data : []);
      setProfitComparison(Array.isArray(profit.data) ? profit.data : []);
    } catch (error) {
      console.error('Error fetching group analytics:', error);
      setTopPerformingGroups([]);
      setRevenueContribution([]);
      setProfitComparison([]);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return `₹${value.toLocaleString()}`;
  };

  const formatNumber = (value: number) => {
    return value.toLocaleString();
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const topPerformingGroupsArray = Array.isArray(topPerformingGroups) ? topPerformingGroups : [];
  const revenueContributionArray = Array.isArray(revenueContribution) ? revenueContribution : [];
  const profitComparisonArray = Array.isArray(profitComparison) ? profitComparison : [];

  const filteredTopPerforming = topPerformingGroupsArray.filter(g => g.groupName !== 'Ungrouped SKUs');
  const filteredRevenue = revenueContributionArray.filter(g => g.groupName !== 'Ungrouped SKUs');
  const filteredProfit = profitComparisonArray.filter(g => g.groupName !== 'Ungrouped SKUs');

  if (!Array.isArray(topPerformingGroups) || !Array.isArray(revenueContribution) || !Array.isArray(profitComparison)) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-500">
        <div className="text-center">
          <p>No group data available</p>
          <p className="text-sm">Upload SKU groups to see analytics</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Date Range Selector */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Group Analytics Date Range</h3>
        <div className="flex gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input
              type="date"
              value={dateRange.start}
              onChange={(e) => setDateRange(prev => ({ ...prev, start: e.target.value }))}
              className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
            <input
              type="date"
              value={dateRange.end}
              onChange={(e) => setDateRange(prev => ({ ...prev, end: e.target.value }))}
              className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Top Performing Groups by Orders */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Top Performing Groups by Orders</h3>
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={filteredTopPerforming.slice(0, 10)}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis 
              dataKey="groupName" 
              angle={-45}
              textAnchor="end"
              height={100}
              interval={0}
            />
            <YAxis />
            <Tooltip 
              formatter={(value, name) => [
                name === 'orderCount' ? formatNumber(value as number) : formatCurrency(value as number),
                name === 'orderCount' ? 'Orders' : name === 'totalRevenue' ? 'Revenue' : 'Profit'
              ]}
            />
            <Legend />
            <Bar dataKey="orderCount" fill="#3B82F6" name="Orders" />
            <Bar dataKey="totalRevenue" fill="#10B981" name="Revenue" />
            <Bar dataKey="totalProfit" fill="#F59E0B" name="Profit" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Revenue Contribution by Group */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Revenue Contribution by Group</h3>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Pie Chart */}
          <div>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={filteredRevenue.slice(0, 8)}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ groupName, percent }) => `${groupName} (${(percent * 100).toFixed(0)}%)`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="revenue"
                >
                  {filteredRevenue.slice(0, 8).map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatCurrency(value as number)} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* Revenue Table */}
          <div>
            <div className="space-y-3">
              {filteredRevenue.slice(0, 8).map((group, index) => (
                <div key={group.groupName} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center">
                    <div 
                      className="w-4 h-4 rounded-full mr-3"
                      style={{ backgroundColor: COLORS[index % COLORS.length] }}
                    ></div>
                    <span className="font-medium text-gray-900">{group.groupName}</span>
                  </div>
                  <span className="font-semibold text-green-600">{formatCurrency(group.revenue)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Profit Comparison Across Groups */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Profit Comparison Across Groups</h3>
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={filteredProfit.slice(0, 10)}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis 
              dataKey="groupName" 
              angle={-45}
              textAnchor="end"
              height={100}
              interval={0}
            />
            <YAxis />
            <Tooltip 
              formatter={(value, name) => [
                name === 'orderCount' ? formatNumber(value as number) : formatCurrency(value as number),
                name === 'orderCount' ? 'Orders' : name === 'totalRevenue' ? 'Revenue' : 'Profit'
              ]}
            />
            <Legend />
            <Bar dataKey="totalProfit" fill="#EF4444" name="Profit" />
            <Bar dataKey="totalRevenue" fill="#10B981" name="Revenue" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Group Performance Summary */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Group Performance Summary</h3>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Rank</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Group Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Orders</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Revenue</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Profit</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Profit Margin</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredTopPerforming.slice(0, 10).map((group, index) => {
                const profitMargin = group.totalRevenue > 0 ? (group.totalProfit / group.totalRevenue) * 100 : 0;
                return (
                  <tr key={group.groupName} className={index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        #{index + 1}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{group.groupName}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{formatNumber(group.orderCount)}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{formatNumber(group.totalQuantity)}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{formatCurrency(group.totalRevenue)}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className={`text-sm font-medium ${
                        group.totalProfit >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {formatCurrency(group.totalProfit)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className={`text-sm font-medium ${
                        profitMargin >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {profitMargin.toFixed(1)}%
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default SkuGroupCharts;
