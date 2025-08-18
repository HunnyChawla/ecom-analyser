import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { TrendingDown, Calendar, DollarSign, Package, AlertTriangle } from 'lucide-react';

const api = axios.create({ baseURL: 'http://localhost:8080' });

interface LossOrder {
  orderId: string;
  skuId: string;
  quantity: number;
  settlementAmount: number;
  purchasePrice: number;
  cogs: number;
  lossAmount: number;
  orderStatus: string;
  orderDate: string;
  productName?: string;
  customerState?: string;
}

const LossAnalysisTable: React.FC = () => {
  const [lossOrders, setLossOrders] = useState<LossOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [summary, setSummary] = useState({
    totalOrders: 0,
    totalQuantity: 0,
    totalRevenue: 0,
    totalCogs: 0,
    totalLoss: 0
  });

  useEffect(() => {
    fetchLossData();
  }, [selectedMonth, selectedYear]);

  const fetchLossData = async () => {
    try {
      setLoading(true);
      
      // Calculate start and end dates for the selected month
      const startDate = new Date(selectedYear, selectedMonth - 1, 1);
      const endDate = new Date(selectedYear, selectedMonth, 0);
      
      const startStr = startDate.toISOString().split('T')[0];
      const endStr = endDate.toISOString().split('T')[0];
      
      const response = await api.get(`/api/analytics/loss-orders?start=${startStr}&end=${endStr}`);
      
      if (response.data && Array.isArray(response.data.orders)) {
        setLossOrders(response.data.orders);
        setSummary(response.data.summary);
      } else {
        setLossOrders([]);
        setSummary({
          totalOrders: 0,
          totalQuantity: 0,
          totalRevenue: 0,
          totalCogs: 0,
          totalLoss: 0
        });
      }
    } catch (error) {
      console.error('Error fetching loss data:', error);
      setLossOrders([]);
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

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN');
  };

  const getMonthName = (month: number) => {
    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return months[month - 1];
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <TrendingDown className="h-8 w-8 text-red-600 mr-3" />
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Loss Analysis</h2>
              <p className="text-gray-600">Orders that resulted in losses despite being delivered and paid</p>
            </div>
          </div>
        </div>

        {/* Month/Year Selector */}
        <div className="flex items-center space-x-4 mb-6">
          <div className="flex items-center space-x-2">
            <Calendar className="h-5 w-5 text-gray-500" />
            <span className="text-sm font-medium text-gray-700">Period:</span>
          </div>
          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(Number(e.target.value))}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            {Array.from({ length: 12 }, (_, i) => (
              <option key={i + 1} value={i + 1}>
                {getMonthName(i + 1)}
              </option>
            ))}
          </select>
          <select
            value={selectedYear}
            onChange={(e) => setSelectedYear(Number(e.target.value))}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            {Array.from({ length: 5 }, (_, i) => {
              const year = new Date().getFullYear() - 2 + i;
              return (
                <option key={year} value={year}>
                  {year}
                </option>
              );
            })}
          </select>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center">
              <AlertTriangle className="h-6 w-6 text-red-600 mr-3" />
              <div>
                <p className="text-sm font-medium text-red-600">Loss Orders</p>
                <p className="text-2xl font-bold text-red-900">{formatNumber(summary.totalOrders)}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center">
              <Package className="h-6 w-6 text-blue-600 mr-3" />
              <div>
                <p className="text-sm font-medium text-blue-600">Total Quantity</p>
                <p className="text-2xl font-bold text-blue-900">{formatNumber(summary.totalQuantity)}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="flex items-center">
              <DollarSign className="h-6 w-6 text-green-600 mr-3" />
              <div>
                <p className="text-sm font-medium text-green-600">Total Revenue</p>
                <p className="text-2xl font-bold text-green-900">{formatCurrency(summary.totalRevenue)}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
            <div className="flex items-center">
              <Package className="h-6 w-6 text-orange-600 mr-3" />
              <div>
                <p className="text-sm font-medium text-orange-600">Total COGS</p>
                <p className="text-2xl font-bold text-orange-900">{formatCurrency(summary.totalCogs)}</p>
              </div>
            </div>
          </div>
          
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center">
              <TrendingDown className="h-6 w-6 text-red-600 mr-3" />
              <div>
                <p className="text-sm font-medium text-red-600">Total Loss</p>
                <p className="text-2xl font-bold text-red-900">{formatCurrency(summary.totalLoss)}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Loss Orders Table */}
      <div className="overflow-x-auto">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading loss analysis data...</p>
            </div>
          </div>
        ) : lossOrders.length === 0 ? (
          <div className="text-center py-12">
            <TrendingDown className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No Loss Orders Found</h3>
            <p className="text-gray-500">
              No orders resulted in losses for {getMonthName(selectedMonth)} {selectedYear}
            </p>
          </div>
        ) : (
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  SKU
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Revenue
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Purchase Price
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  COGS
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Loss Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order Date
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {lossOrders.map((order, index) => (
                <tr key={`${order.orderId}-${index}`} className="hover:bg-red-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900 font-mono">
                      {order.orderId}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900 font-mono">
                      {order.skuId}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {formatNumber(order.quantity)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-green-600">
                      {formatCurrency(order.settlementAmount)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {formatCurrency(order.purchasePrice)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-orange-600">
                      {formatCurrency(order.cogs)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-bold text-red-600">
                      {formatCurrency(order.lossAmount)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {formatDate(order.orderDate)}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default LossAnalysisTable;
