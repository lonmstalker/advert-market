import { Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import type { ChannelDetail } from '@/features/channels';
import { RuleIndicator } from './RuleIndicator';

type ChannelRulesSectionProps = {
  rules: NonNullable<ChannelDetail['rules']>;
};

const ruleItemStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'flex-start',
  gap: 8,
};

export function ChannelRulesSection({ rules }: ChannelRulesSectionProps) {
  const { t } = useTranslation();

  const hasContent = rules.maxPostChars != null || (rules.prohibitedTopics && rules.prohibitedTopics.length > 0);
  const hasMedia = rules.mediaAllowed != null || rules.mediaTypes != null || rules.maxMediaCount != null;
  const hasLinksButtons = rules.linksAllowed != null || rules.mentionsAllowed != null || rules.maxButtons != null;
  const hasFormatting = rules.formattingAllowed != null;
  const sections: { label: string; rows: React.ReactNode[] }[] = [];

  if (hasContent) {
    const rows: React.ReactNode[] = [];
    if (rules.maxPostChars != null) {
      rows.push(
        <div key="chars" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">{t('catalog.channel.rulesMaxChars', { count: rules.maxPostChars })}</Text>
        </div>,
      );
    }
    if (rules.prohibitedTopics && rules.prohibitedTopics.length > 0) {
      rows.push(
        <div key="prohibited" style={{ ...ruleItemStyle, flexWrap: 'wrap' }}>
          <RuleIndicator allowed={false} />
          <Text type="caption1" color="secondary">
            {t('catalog.channel.rulesProhibited')}:
          </Text>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, width: '100%', paddingLeft: 26 }}>
            {rules.prohibitedTopics.map((topic) => (
              <span
                key={topic}
                style={{
                  padding: '2px 8px',
                  borderRadius: 6,
                  background: 'color-mix(in srgb, var(--color-state-destructive) 8%, transparent)',
                  fontSize: 12,
                  fontWeight: 500,
                  color: 'var(--color-state-destructive)',
                }}
              >
                {topic}
              </span>
            ))}
          </div>
        </div>,
      );
    }
    sections.push({ label: t('catalog.channel.rulesPostSize'), rows });
  }

  if (hasMedia) {
    const rows: React.ReactNode[] = [];
    if (rules.mediaAllowed != null) {
      rows.push(
        <div key="media" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.mediaAllowed} />
          <Text type="caption1">
            {rules.mediaAllowed ? t('catalog.channel.rulesMediaAllowed') : t('catalog.channel.rulesMediaNotAllowed')}
          </Text>
        </div>,
      );
    }
    if (rules.mediaTypes && rules.mediaTypes.length > 0) {
      const types = rules.mediaTypes.map((mt) => t(`catalog.channel.mediaType.${mt}`, { defaultValue: mt })).join(', ');
      rows.push(
        <div key="types" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">{t('catalog.channel.rulesMediaTypes', { types })}</Text>
        </div>,
      );
    }
    if (rules.maxMediaCount != null) {
      rows.push(
        <div key="count" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">{t('catalog.channel.rulesMaxMedia', { count: rules.maxMediaCount })}</Text>
        </div>,
      );
    }
    sections.push({ label: t('catalog.channel.rulesMedia'), rows });
  }

  if (hasLinksButtons) {
    const rows: React.ReactNode[] = [];
    if (rules.linksAllowed != null) {
      rows.push(
        <div key="links" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.linksAllowed} />
          <Text type="caption1">
            {rules.linksAllowed ? t('catalog.channel.rulesLinksAllowed') : t('catalog.channel.rulesLinksNotAllowed')}
          </Text>
        </div>,
      );
    }
    if (rules.mentionsAllowed != null) {
      rows.push(
        <div key="mentions" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.mentionsAllowed} />
          <Text type="caption1">
            {rules.mentionsAllowed
              ? t('catalog.channel.rulesMentionsAllowed')
              : t('catalog.channel.rulesMentionsNotAllowed')}
          </Text>
        </div>,
      );
    }
    if (rules.maxButtons != null) {
      rows.push(
        <div key="buttons" style={ruleItemStyle}>
          <RuleIndicator allowed />
          <Text type="caption1">
            {t('catalog.channel.rulesMaxButtons')}:{' '}
            {t('catalog.channel.rulesButtonsCount', { count: rules.maxButtons })}
          </Text>
        </div>,
      );
    }
    sections.push({ label: t('catalog.channel.rulesLinks'), rows });
  }

  if (hasFormatting) {
    sections.push({
      label: t('catalog.channel.rulesFormatting'),
      rows: [
        <div key="fmt" style={ruleItemStyle}>
          <RuleIndicator allowed={rules.formattingAllowed as boolean} />
          <Text type="caption1">
            {rules.formattingAllowed
              ? t('catalog.channel.rulesFormattingAllowed')
              : t('catalog.channel.rulesFormattingNotAllowed')}
          </Text>
        </div>,
      ],
    });
  }

  if (sections.length === 0) {
    return (
      <div style={{ padding: '12px 16px', background: 'var(--color-background-section)', borderRadius: 12 }}>
        <Text type="caption1" color="tertiary">
          {t('catalog.channel.noRules')}
        </Text>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {sections.length > 0 && (
        <div
          style={{
            background: 'var(--color-background-base)',
            border: '1px solid var(--color-border-separator)',
            borderRadius: 12,
            overflow: 'hidden',
          }}
        >
          {sections.map((section, i) => (
            <div
              key={section.label}
              style={{
                padding: '12px 16px',
                borderBottom: i < sections.length - 1 ? '1px solid var(--color-border-separator)' : 'none',
              }}
            >
              <Text type="caption1" weight="medium" color="secondary" style={{ marginBottom: 8 }}>
                {section.label}
              </Text>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>{section.rows}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
