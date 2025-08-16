# EcomAnalyser - E-commerce Analytics Portal

A comprehensive web portal for analyzing order and payment data from Excel/CSV files and visualizing key business metrics using charts.

## 🚀 Features

### Core Analytics
- **Orders by Timeframe**: Aggregated by day, month, quarter, and year
- **Payments by Timeframe**: Total payment amounts over time
- **Top Ordered Items**: Based on quantity sold
- **Top Profitable SKUs**: Based on profit calculation
- **Profit Trends**: Profit aggregation over time
- **Loss Trends**: Loss aggregation over time

### Advanced Features
- **SKU Group Management**: Group SKUs and apply group-level pricing
- **Data Merge**: Intelligent merging of order and payment data
- **Data Quality Monitoring**: SKU coverage and data integrity metrics
- **CSV/Excel Support**: Handle both file formats seamlessly

### Data Management
- **File Upload**: Support for orders, payments, and SKU price files
- **Duplicate Prevention**: Upsert logic to handle data updates
- **Data Validation**: Comprehensive error checking and logging
- **Export Functionality**: CSV export of merged data

## 🏗️ Architecture

### Backend (Spring Boot)
- **Java 17** with **Spring Boot 3.x**
- **JPA/Hibernate** with **PostgreSQL** database
- **Apache POI** for Excel file parsing
- **Apache Commons CSV** for CSV file parsing
- **RESTful APIs** with comprehensive documentation
- **Docker** containerization

### Frontend (React.js)
- **React 18** with **TypeScript**
- **Vite** for fast development and building
- **TailwindCSS** for modern, responsive UI
- **Recharts** for data visualization
- **Axios** for API communication

## 📋 Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **Docker** and **Docker Compose**
- **PostgreSQL** (or use Docker)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone git@github.com:HunnyChawla/ecom-analyser.git
cd ecom-analyser
```

### 2. Start Backend Services
```bash
cd backend
docker-compose up --build -d
```

The backend will be available at `http://localhost:8080`

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at `http://localhost:5173`

## 📊 API Endpoints

### File Upload
- `POST /api/upload/orders` - Upload order data
- `POST /api/upload/payments` - Upload payment data
- `POST /api/upload/sku-prices` - Upload SKU pricing

### Analytics
- `GET /api/analytics/orders-by-timeframe` - Orders over time
- `GET /api/analytics/payments-by-timeframe` - Payments over time
- `GET /api/analytics/top-ordered-items` - Top ordered SKUs
- `GET /api/analytics/top-profitable-skus` - Most profitable SKUs
- `GET /api/analytics/profit-trends` - Profit trends over time
- `GET /api/analytics/loss-trends` - Loss trends over time
- `GET /api/analytics/orders-by-status` - Orders by status

### SKU Groups
- `POST /api/sku-groups/upload` - Upload SKU group definitions
- `GET /api/sku-groups/template` - Download SKU group template
- `GET /api/sku-groups/analytics/*` - Group analytics endpoints

### Data Merge
- `GET /api/data-merge/merged-data/paginated` - Paginated merged data
- `GET /api/data-merge/statistics` - Merge statistics and data quality
- `GET /api/data-merge/merged-data/status/{status}` - Filter by status

## 🗄️ Database Schema

### Core Tables
- **orders**: Order information with SKU, quantity, pricing
- **payments**: Payment details and order status
- **sku_prices**: SKU purchase prices for profit calculation

### SKU Groups
- **sku_groups**: Group definitions with purchase prices
- **sku_group_mappings**: SKU to group relationships

## 📁 Project Structure

```
EcomAnalyser/
├── backend/                 # Spring Boot backend
│   ├── src/main/java/
│   │   ├── controller/     # REST controllers
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Data access layer
│   │   └── domain/         # JPA entities
│   ├── src/main/resources/
│   │   ├── application.yml # Configuration
│   │   └── schema.sql      # Database schema
│   ├── Dockerfile          # Backend container
│   └── docker-compose.yml  # Service orchestration
├── frontend/               # React.js frontend
│   ├── src/
│   │   ├── components/     # Reusable components
│   │   ├── pages/          # Page components
│   │   └── App.tsx         # Main application
│   ├── package.json        # Dependencies
│   └── vite.config.ts      # Build configuration
├── data/                   # Sample data files
└── README.md              # This file
```

## 🔧 Configuration

### Backend Configuration
- Database connection in `application.yml`
- Logging levels and file paths
- File upload size limits

### Frontend Configuration
- API proxy settings in `vite.config.ts`
- Environment variables for different deployments

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 🐳 Docker

### Backend Container
```bash
cd backend
docker build -t ecomanalyser-backend .
docker run -p 8080:8080 ecomanalyser-backend
```

### Full Stack with Docker Compose
```bash
docker-compose up --build
```

## 📈 Data Quality Features

- **SKU Coverage Monitoring**: Track percentage of records with SKU information
- **Data Integrity Checks**: Validate data consistency between orders and payments
- **Duplicate Detection**: Prevent duplicate data with upsert logic
- **Comprehensive Logging**: Detailed logs for debugging and monitoring

## 🚨 Troubleshooting

### Common Issues

1. **Upload Orders API 500 Error**
   - Check for duplicate order IDs
   - Verify CSV/Excel format matches expected structure

2. **Missing SKU Information**
   - Ensure complete order file is uploaded
   - Check data quality metrics in Data Merge page

3. **Database Connection Issues**
   - Verify PostgreSQL is running
   - Check connection settings in `application.yml`

### Logs
- Backend logs: `docker logs ecomanalyser_backend`
- Database logs: `docker logs ecomanalyser_db`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For support and questions, please open an issue on GitHub or contact the development team.

---

**Built with ❤️ using Spring Boot and React.js**


