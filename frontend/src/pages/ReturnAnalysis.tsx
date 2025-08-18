import React from 'react';
import ReturnAnalysisTable from '../components/ReturnAnalysisTable';

const ReturnAnalysis: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Return Analysis</h1>
          <p className="text-gray-600">
            Analyze orders with negative settlement amounts and non-delivered statuses. 
            This helps identify returns, cancellations, and other order issues that result in financial losses.
          </p>
        </div>
        <ReturnAnalysisTable />
      </div>
    </div>
  );
};

export default ReturnAnalysis;
