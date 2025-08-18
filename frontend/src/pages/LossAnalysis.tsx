import React from 'react';
import LossAnalysisTable from '../components/LossAnalysisTable';

const LossAnalysis: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Loss Analysis</h1>
          <p className="text-gray-600">
            Analyze orders that resulted in losses despite being delivered and having payments settled. 
            This helps identify products with pricing issues or high costs that need attention.
          </p>
        </div>

        {/* Loss Analysis Table Component */}
        <LossAnalysisTable />
      </div>
    </div>
  );
};

export default LossAnalysis;
