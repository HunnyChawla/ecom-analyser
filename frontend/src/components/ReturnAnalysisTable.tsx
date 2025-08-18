import React, { useState, useEffect } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: 'http://localhost:8080' });

interface ReturnOrder {
  orderId: string;
  skuId: string;
  quantity: number;
  settlementAmount: number;
  returnAmount: number;
  purchasePrice: number;
  cogs: number;
  loss: number;
  orderStatus: string;
  orderDate: string;
  isUnexpectedStatus: boolean;
}

interface ReturnSummary {
  totalOrders: number;
  totalQuantity: number;
  totalReturnAmount: number;
  totalCogs: number;
  totalLoss: number;
  unexpectedStatuses: string[];
}

const ReturnAnalysisTable: React.FC = () => {
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [returnData, setReturnData] = useState<ReturnOrder[]>([]);
  const [summary, setSummary] = useState<ReturnSummary | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string>('');

  const fetchReturnData = async () => {
    setIsLoading(true);
    setError('');
    
    try {
      const startDate = new Date(selectedYear, selectedMonth - 1, 1);
      const endDate = new Date(selectedYear, selectedMonth, 0);
      
      const startStr = startDate.toLocaleDateString('en-CA');
      const endStr = endDate.toLocaleDateString('en-CA');
      
      const response = await api.get(`/api/analytics/return-analysis?start=${startStr}&end=${endStr}`);
      
      setReturnData(response.data.orders || []);
      setSummary(response.data.summary || null);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch return data');
      console.error('Error fetching return data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReturnData();
  }, [selectedYear, selectedMonth]);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-IN');
  };

  const getStatusBadgeClass = (status: string, isUnexpected: boolean) => {
    if (isUnexpected) {
      return 'bg-red-100 text-red-800 border-red-200';
    }
    
    const statusLower = status.toLowerCase();
    if (statusLower.includes('return') || statusLower.includes('rto')) {
      return 'bg-blue-100 text-blue-800 border-blue-200';
    } else if (statusLower.includes('cancelled')) {
      return 'bg-gray-100 text-gray-800 border-gray-200';
    } else if (statusLower.includes('refund')) {
      return 'bg-purple-100 text-purple-800 border-purple-200';
    } else if (statusLower.includes('rejected') || statusLower.includes('failed')) {
      return 'bg-orange-100 text-orange-800 border-orange-200';
    }
    
    return 'bg-gray-100 text-gray-800 border-gray-200';
  };

  const years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);
  const months = [
    { value: 1, name: 'January' },
    { value: 2, name: 'February' },
    { value: 3, name: 'March' },
    { value: 4, name: 'April' },
    { value: 5, name: 'May' },
    { value: 6, name: 'June' },
    { value: 7, name: 'July' },
    { value: 8, name: 'August' },
    { value: 9, name: 'September' },
    { value: 10, name: 'October' },
    { value: 11, name: 'November' },
    { value: 12, name: 'December' }
  ];

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      {/* Header and Controls */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Return Analysis</h2>
          <p className="text-gray-600 mt-1">
            Analyze returns and identify orders with negative settlement amounts
          </p>
        </div>
        
        <div className="flex gap-3">
          <select
            value={selectedYear}
            onChange={(e) => setSelectedYear(Number(e.target.value))}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {years.map(year => (
              <option key={year} value={year}>{year}</option>
            ))}
          </select>
          
          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(Number(e.target.value))}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {months.map(month => (
              <option key={month.value} value={month.value}>{month.name}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="text-blue-600 text-sm font-medium">Total Returns</div>
            <div className="text-2xl font-bold text-blue-900">{summary.totalOrders}</div>
            <div className="text-blue-600 text-sm">Orders</div>
          </div>
          
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="text-green-600 text-sm font-medium">Total Quantity</div>
            <div className="text-2xl font-bold text-green-900">{summary.totalQuantity}</div>
            <div className="text-green-600 text-sm">Items</div>
          </div>
          
          <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
            <div className="text-purple-600 text-sm font-medium">Return Amount</div>
            <div className="text-2xl font-bold text-purple-900">{formatCurrency(summary.totalReturnAmount)}</div>
            <div className="text-purple-600 text-sm">Total Refunds</div>
          </div>
          
          <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
            <div className="text-orange-600 text-sm font-medium">Total COGS</div>
            <div className="text-2xl font-bold text-orange-900">{formatCurrency(summary.totalCogs)}</div>
            <div className="text-orange-600 text-sm">Cost of Goods</div>
          </div>
          
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="text-red-600 text-sm font-medium">Total Loss</div>
            <div className="text-2xl font-bold text-red-900">{formatCurrency(summary.totalLoss)}</div>
            <div className="text-red-600 text-sm">From Returns</div>
          </div>
        </div>
      )}

      {/* Unexpected Statuses Warning */}
      {summary && summary.unexpectedStatuses.length > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-yellow-800">
                Unexpected Statuses Detected
              </h3>
              <div className="mt-2 text-sm text-yellow-700">
                <p>The following order statuses were found that are not typically associated with returns:</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {summary.unexpectedStatuses.map((status, index) => (
                    <span key={index} className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                      {status}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="flex justify-center items-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-2 text-gray-600">Loading return data...</span>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <div className="text-red-800">
            <strong>Error:</strong> {error}
          </div>
        </div>
      )}

      {/* Return Orders Table */}
      {!isLoading && !error && returnData.length > 0 && (
        <div className="overflow-x-auto">
          {/* Sorting Information */}
          <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="text-sm text-blue-800">
              <strong>📊 Table Sorting:</strong> Orders with unexpected statuses (highlighted in red) are displayed first, followed by standard return statuses sorted by return amount (highest first).
            </div>
          </div>
          
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order Details
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  SKU & Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Financial Impact
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status & Date
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {returnData.map((order, index) => (
                <tr key={index} className={order.isUnexpectedStatus ? 'bg-red-100 border-l-4 border-l-red-500' : 'hover:bg-gray-50'}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{order.orderId}</div>
                    {order.isUnexpectedStatus && (
                      <div className="text-xs text-red-700 font-bold mt-1 flex items-center">
                        <span className="mr-1">🚨</span>
                        UNEXPECTED STATUS - REQUIRES ATTENTION
                      </div>
                    )}
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{order.skuId}</div>
                    <div className="text-sm text-gray-500">Qty: {order.quantity}</div>
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm">
                      <span className="text-red-600 font-medium">
                        Return: {formatCurrency(order.returnAmount)}
                      </span>
                    </div>
                    <div className="text-sm text-gray-500">
                      COGS: {formatCurrency(order.cogs)}
                    </div>
                    <div className="text-sm text-red-600 font-medium">
                      Loss: {formatCurrency(order.loss)}
                    </div>
                  </td>
                  
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getStatusBadgeClass(order.orderStatus, order.isUnexpectedStatus)}`}>
                      {order.orderStatus}
                    </span>
                    <div className="text-sm text-gray-500 mt-1">
                      {formatDate(order.orderDate)}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* No Data State */}
      {!isLoading && !error && returnData.length === 0 && (
        <div className="text-center py-8">
          <div className="text-gray-500 text-lg">No return data found for the selected period</div>
          <div className="text-gray-400 text-sm mt-2">
            Try selecting a different month or year
          </div>
        </div>
      )}
    </div>
  );
};

export default ReturnAnalysisTable;
