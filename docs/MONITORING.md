# User Manager - Monitoring Dashboard

## Build Status
[![CircleCI](https://dl.circleci.com/status-badge/img/circleci/9S3XviWTB3m5oEDfR2c5gT/SgeB5oGNZF8Np1AhpjjwR6/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/circleci/9S3XviWTB3m5oEDfR2c5gT/SgeB5oGNZF8Np1AhpjjwR6/tree/main)

## Code Quality
[![codecov](https://codecov.io/gitlab/lpreaux/usermanager/graph/badge.svg?token=7D5GDS8H1G)](https://codecov.io/gitlab/lpreaux/usermanager)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=your-project-key&metric=alert_status)](https://sonarcloud.io/dashboard?id=your-project-key)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=your-project-key&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=your-project-key)

## Test Coverage by Layer (Architecture Hexagonale)

| Layer | Coverage | Description |
|-------|----------|-------------|
| Domain | ![Domain Coverage](https://img.shields.io/badge/coverage-95%25-brightgreen) | Value Objects, Entities |
| Application | ![Application Coverage](https://img.shields.io/badge/coverage-85%25-green) | Use Cases, Services |
| Infrastructure | ![Infrastructure Coverage](https://img.shields.io/badge/coverage-75%25-yellowgreen) | Adapters, Controllers |

## Metrics

### Code Quality Metrics
- **Bugs**: 0
- **Vulnerabilities**: 0
- **Code Smells**: 5
- **Technical Debt**: 2h
- **Duplicated Lines**: 1.2%

### Test Metrics
- **Total Tests**: 45
- **Unit Tests**: 35
- **Integration Tests**: 10
- **Test Execution Time**: ~30s

## Monitoring Links

- [CircleCI Dashboard](https://app.circleci.com/pipelines/github/your-username/user-manager)
- [Codecov Reports](https://codecov.io/gh/your-username/user-manager)
- [SonarCloud Dashboard](https://sonarcloud.io/dashboard?id=your-project-key)

## Architecture Hexagonale - Test Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Test Pyramid                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│    ┌───────────────────────────────────────┐                │
│    │        Integration Tests (10%)        │  ← Full flow   │
│    └───────────────────────────────────────┘                │
│                                                             │
│    ┌───────────────────────────────────────┐                │
│    │     Application Tests (30%)           │  ← Use Cases   │
│    └───────────────────────────────────────┘                │
│                                                             │
│    ┌───────────────────────────────────────┐                │
│    │        Domain Tests (60%)             │  ← Pure logic  │
│    └───────────────────────────────────────┘               s │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Alerting

Configured alerts:
- Build failure
- Coverage drop > 5%
- New security vulnerabilities
- Performance regression