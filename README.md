# Candle Aggregation Service

A Java service that processes real-time bid/ask market data and aggregates it into candlestick (OHLC) format for charting applications.

## 🚀 Features

- Real-time market data processing with OHLC candlestick aggregation
- Multiple time intervals (1s, 1m, 5m, 15m, 1h, 4h, 1d)
- Multi-symbol support (BTC-USD, ETH-USD, AAPL, GOOGL, TSLA)
- Asynchronous processing with thread pools
- H2 in-memory database with batch operations
- RESTful API for historical data retrieval
- Health monitoring endpoints
- Built-in data simulator for testing

## 📋 Prerequisites

- Java 21+
- Gradle 8.0+

## 🛠️ Quick Start

```bash
# Clone and build
git clone https://github.com/jhamdheeraj/candle-aggregation-service
cd candle-aggregation-service
./gradlew clean build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test
```

Application starts on `http://localhost:8080`

## 📡 API

### Historical Candle Data
```http
GET /api/v1/candle-aggregator/history?symbol={symbol}&interval={interval}&from={timestamp}&to={timestamp}
```

**Parameters:**
- `symbol`: Trading symbol (BTC-USD, ETH-USD, etc.)
- `interval`: Time interval (1s, 1m, 5m, 15m, 1h, 4h, 1d)
- `from`: Start timestamp (Unix milliseconds)
- `to`: End timestamp (Unix milliseconds)

**Example:**
```bash
curl "http://localhost:8080/api/v1/candle-aggregator/history?symbol=BTC-USD&interval=1m&from=1640995200000&to=1641081600000"
```

**Response:**
```json
{
  "s": "ok",
  "c": [30016.45],
  "t": [1772353320],
  "v": [96],
  "h": [30099.97],
  "l": [30001.41],
  "o": [30069.82]
}
```

### Health Checks
```http
GET /health                   # Detailed application health
GET /actuator/health          # Actuator health
```

## ⚙️ Configuration

Key settings in `application.yaml`:

```yaml
candle:
  aggregation:
    intervals: 1s,1m,5m,15m,1h,4h,1d
    supported-symbols: BTC-USD,ETH-USD,AAPL,GOOGL,TSLA
    flush-rate-ms: 1000

spring:
  datasource:
    url: jdbc:h2:mem:candles
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
```

## 🏗️ Architecture

**Data Flow:**
1. Market events processed asynchronously by symbol/interval
2. OHLC values calculated and updated in real-time
3. Completed candles persisted to H2 database in batches
4. Historical data retrieved via REST API

**Key Components:**
- `CandleAggregationService`: Core aggregation logic
- `CandleDataSimulatorService`: Test data generation
- `CandleHistoryService`: Historical data retrieval
- `CandlePersistenceService`: Database operations

## 🧪 Testing

```bash
./gradlew test
```

Test coverage includes:
- Candle creation and OHLC calculations
- Concurrent processing scenarios
- Error handling and validation
- Database operations

## 🔧 Development Tools

- **H2 Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:candles`, username: `sa`)
- **Actuator Endpoints**: Spring Boot monitoring
- **Data Simulator**: Built-in market data generation

## 📊 Trade-offs

**H2 In-Memory Database:**
- ✅ Fast performance, zero setup
- ❌ Data lost on restart, not production-ready

**Fixed Thread Pools:**
- ✅ Predictable resource usage
- ❌ Less flexible for variable loads

**Batch Processing:**
- ✅ Improved throughput
- ❌ Increased latency (50 records/batch) for development environment

## 🚀 Deployment

### Environment Variables
```bash
SERVER_PORT=8080
CANDLE_AGGREGATION_SUPPORTED_SYMBOLS=BTC-USD,ETH-USD
```

## 🆘 Support

For issues:
1. Check H2 console for database issues
2. Review application logs
3. Verify configuration parameters
4. Run health check endpoints

---

**Note**: Designed for development and testing. Use persistent database and security measures for production.
