# Patent & Expert Management System

## Features
1. **Patent Database & Search**: Store and search patents by title or abstract.
2. **Expert Database & Search**: Store and search experts by name or field.
3. **Smart Matching**: Automatically match requirements to relevant patents and experts.
4. **Intelligent Valuation**: Automatically evaluate patent value based on content and metadata.
5. **Transformation Results**: Track and display successful patent transformations.

## API Endpoints

Full API documentation: `docs/api.md`

### Patents
- `GET /api/patents?category={wind|solar|biomass|hydrogen|lilon}&query={keyword}` - Search patents by category
- `POST /api/patents?category={wind|solar|biomass|hydrogen|lilon}` - Create or update a patent by category
- `GET /api/patents/{category}/{publicNum}` - Get a patent by category and public_num

### Experts
- `GET /api/experts?query={keyword}` - Search experts
- `POST /api/experts` - Create an expert

### Requirements & Matching
- `POST /api/requirements` - Create a requirement
- `GET /api/requirements/{id}/match-patents` - Find matching patents for a requirement
- `GET /api/requirements/{id}/match-experts` - Find matching experts for a requirement

### Transformation Results
- `GET /api/transformations` - List all transformation results
- `POST /api/transformations` - Record a new result

## Configuration
默认使用本地内存库（profile=local）启动；连接 MySQL 时使用 profile=mysql。

### MySQL（profile=mysql）
方式一：直接修改 `src/main/resources/application-mysql.properties`。

方式二：使用环境变量（推荐）：
- `DB_URL`（可选）
- `DB_USERNAME`（可选）
- `DB_PASSWORD`（可选）

启动示例：
```bash
./gradlew bootRun --args="--spring.profiles.active=mysql"
```

## Running
```bash
./gradlew bootRun
```
