import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { 
  Upload, 
  Download, 
  BarChart3, 
  PieChart, 
  Package,
  AlertCircle,
  CheckCircle,
  XCircle
} from 'lucide-react';

const api = axios.create({ baseURL: 'http://localhost:8080' });

interface SkuGroup {
  id: number;
  groupName: string;
  purchasePrice: number;
  description: string;
  createdAt: string;
}

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

const SkuGroupManagement: React.FC = () => {
  const [groups, setGroups] = useState<SkuGroup[]>([]);
  const [ungroupedSkus, setUngroupedSkus] = useState<string[]>([]);
  const [topPerformingGroups, setTopPerformingGroups] = useState<GroupAnalytics[]>([]);
  const [revenueContribution, setRevenueContribution] = useState<RevenueContribution[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'success' | 'error'>('idle');
  const [uploadMessage, setUploadMessage] = useState('');
  const [dateRange, setDateRange] = useState({
    start: new Date(new Date().getFullYear(), 0, 1).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    fetchGroups();
    fetchUngroupedSkus();
    fetchAnalytics();
  }, [dateRange]);

  const fetchGroups = async () => {
    try {
      const response = await api.get('/api/sku-groups');
      setGroups(response.data);
    } catch (error) {
      console.error('Error fetching groups:', error);
      setGroups([]);
    }
  };

  const fetchUngroupedSkus = async () => {
    try {
      const response = await api.get('/api/sku-groups/ungrouped');
      setUngroupedSkus(Array.isArray(response.data?.ungroupedSkus) ? response.data.ungroupedSkus : []);
    } catch (error) {
      console.error('Error fetching ungrouped SKUs:', error);
      setUngroupedSkus([]); // Set empty array on error
    }
  };

  const fetchAnalytics = async () => {
    try {
      const [topPerforming, revenue] = await Promise.all([
        api.get(`/api/sku-groups/analytics/top-performing?start=${dateRange.start}&end=${dateRange.end}`),
        api.get(`/api/sku-groups/analytics/revenue-contribution?start=${dateRange.start}&end=${dateRange.end}`)
      ]);
      setTopPerformingGroups(Array.isArray(topPerforming.data) ? topPerforming.data : []);
      setRevenueContribution(Array.isArray(revenue.data) ? revenue.data : []);
    } catch (error) {
      console.error('Error fetching analytics:', error);
      setTopPerformingGroups([]);
      setRevenueContribution([]);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setUploadStatus('uploading');
    setUploadMessage('Uploading SKU groups...');

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await api.post('/api/sku-groups/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      setUploadStatus('success');
      setUploadMessage(`Successfully imported ${response.data.importedGroups} groups!`);
      setTimeout(() => {
        fetchGroups();
        fetchUngroupedSkus();
        fetchAnalytics();
      }, 1000);
    } catch (error: any) {
      setUploadStatus('error');
      setUploadMessage(error.response?.data?.error || 'Upload failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const downloadTemplate = async () => {
    try {
      const response = await api.get('/api/sku-groups/template', {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'sku_group_template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading template:', error);
    }
  };

  const getStatusIcon = (status: 'idle' | 'uploading' | 'success' | 'error') => {
    switch (status) {
      case 'uploading':
        return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>;
      case 'success':
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      case 'error':
        return <XCircle className="h-4 w-4 text-red-600" />;
      default:
        return null;
    }
  };

  const getStatusColor = (status: 'idle' | 'uploading' | 'success' | 'error') => {
    switch (status) {
      case 'success':
        return 'bg-green-50 border-green-200 text-green-800';
      case 'error':
        return 'bg-red-50 border-red-200 text-red-800';
      case 'uploading':
        return 'bg-blue-50 border-blue-200 text-blue-800';
      default:
        return 'bg-gray-50 border-gray-200 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">SKU Group Management</h1>
          <p className="text-gray-600">Organize your SKUs into logical groups for better analytics and pricing management</p>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="flex items-center justify-center h-64">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading SKU group data...</p>
            </div>
          </div>
        )}

        {/* Content - Only show when not loading */}
        {!loading && (
          <>
            {/* Upload Section */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-xl font-semibold text-gray-900 mb-2">Upload SKU Groups</h2>
                  <p className="text-gray-600">Upload an Excel file to create or update SKU groups</p>
                </div>
                <button
                  onClick={downloadTemplate}
                  className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                  <Download className="h-4 w-4 mr-2" />
                  Download Template
                </button>
              </div>

              <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
                <input
                  type="file"
                  accept=".xlsx,.xls"
                  onChange={handleFileUpload}
                  className="hidden"
                  id="file-upload"
                  disabled={loading}
                />
                <label
                  htmlFor="file-upload"
                  className={`cursor-pointer inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    loading ? 'opacity-50 cursor-not-allowed' : ''
                  }`}
                >
                  <Upload className="h-4 w-4 mr-2" />
                  {loading ? 'Uploading...' : 'Choose File'}
                </label>
                <p className="mt-2 text-sm text-gray-500">
                  Excel files only (.xlsx, .xls)
                </p>
              </div>

              {/* Upload Status */}
              {uploadStatus !== 'idle' && (
                <div className={`mt-4 p-4 rounded-md border ${getStatusColor(uploadStatus)}`}>
                  <div className="flex items-center">
                    {getStatusIcon(uploadStatus)}
                    <span className="ml-2 text-sm font-medium">{uploadMessage}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Analytics Overview */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div className="flex items-center">
                  <div className="p-2 bg-blue-100 rounded-lg">
                    <Package className="h-6 w-6 text-blue-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">Total Groups</p>
                    <p className="text-2xl font-semibold text-gray-900">{Array.isArray(groups) ? groups.length : 0}</p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div className="flex items-center">
                  <div className="p-2 bg-yellow-100 rounded-lg">
                    <AlertCircle className="h-6 w-6 text-yellow-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">Ungrouped SKUs</p>
                    <p className="text-2xl font-semibold text-gray-900">{Array.isArray(ungroupedSkus) ? ungroupedSkus.length : 0}</p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div className="flex items-center">
                  <div className="p-2 bg-green-100 rounded-lg">
                    <BarChart3 className="h-6 w-6 text-green-600" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">Active Groups</p>
                    <p className="text-2xl font-semibold text-gray-900">
                      {Array.isArray(topPerformingGroups) ? topPerformingGroups.filter(g => g.groupName !== 'Ungrouped SKUs').length : 0}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Date Range Selector */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Analytics Date Range</h3>
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

            {/* Analytics Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
              {/* Top Performing Groups */}
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <BarChart3 className="h-5 w-5 mr-2 text-blue-600" />
                  Top Performing Groups by Orders
                </h3>
                <div className="space-y-4">
                  {Array.isArray(topPerformingGroups) && topPerformingGroups.slice(0, 5).map((group, index) => (
                    <div key={group.groupName} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center">
                        <span className="text-sm font-medium text-gray-500 w-6">#{index + 1}</span>
                        <div>
                          <p className="font-medium text-gray-900">{group.groupName}</p>
                          <p className="text-sm text-gray-500">{group.orderCount} orders</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="font-semibold text-gray-900">₹{Number(group.totalRevenue || 0).toLocaleString()}</p>
                        <p className="text-sm text-green-600">₹{Number(group.totalProfit || 0).toLocaleString()}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Revenue Contribution */}
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <PieChart className="h-5 w-5 mr-2 text-green-600" />
                  Revenue Contribution by Group
                </h3>
                <div className="space-y-4">
                  {Array.isArray(revenueContribution) && revenueContribution.slice(0, 5).map((group, index) => (
                    <div key={group.groupName} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center">
                        <span className="text-sm font-medium text-gray-500 w-6">#{index + 1}</span>
                        <p className="font-medium text-gray-900">{group.groupName}</p>
                      </div>
                      <p className="font-semibold text-green-600">₹{Number(group.revenue || 0).toLocaleString()}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* SKU Groups Table */}
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8 col-span-3 lg:col-span-3">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">SKU Groups</h3>
                {!Array.isArray(groups) || groups.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    <Package className="h-12 w-12 mx-auto mb-4 text-gray-300" />
                    <p>No SKU groups created yet. Upload a template to get started.</p>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Group Name</th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Purchase Price</th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
                          <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {groups.map((group) => (
                          <tr key={group.id}>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm font-medium text-gray-900">{group.groupName}</div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-gray-900">₹{Number(group.purchasePrice || 0).toLocaleString()}</div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-gray-500">{group.description}</div>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                              <div className="text-sm text-gray-500">
                                {new Date(group.createdAt).toLocaleDateString()}
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>

            {/* Ungrouped SKUs */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <AlertCircle className="h-5 w-5 mr-2 text-yellow-600" />
                Ungrouped SKUs ({Array.isArray(ungroupedSkus) ? ungroupedSkus.length : 0})
              </h3>
              {!Array.isArray(ungroupedSkus) || ungroupedSkus.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-300" />
                  <p>All SKUs have been assigned to groups!</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2">
                  {ungroupedSkus.slice(0, 20).map((sku) => (
                    <div key={sku} className="p-2 bg-gray-50 rounded text-sm text-gray-600 font-mono">
                      {sku}
                    </div>
                  ))}
                  {ungroupedSkus.length > 20 && (
                    <div className="p-2 bg-blue-50 rounded text-sm text-blue-600">
                      +{ungroupedSkus.length - 20} more...
                    </div>
                  )}
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default SkuGroupManagement;
