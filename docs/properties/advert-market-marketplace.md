# advert-market-marketplace

## Table of Contents

- [Channel Bot](#channel-bot)
- [Team Management](#team-management)


---

## Channel Bot

Bot identity for channel admin verification


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.marketplace.channel.bot-user-id` | `Positive long` | Telegram user ID of the bot |  | Yes |  |  |
| `app.marketplace.channel.verification-timeout` | `Duration` | Timeout for Telegram API calls during channel verification |  | No |  |  |

## Team Management

Limits for channel team management


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.marketplace.team.max-managers` | `Positive int` | Maximum number of managers per channel |  | Yes |  |  |
