import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { useTranslation } from 'react-i18next';
import type { ChannelDetail } from '@/features/channels';
import { RuleIndicator } from './RuleIndicator';

type ChannelRulesSectionProps = {
  rules: NonNullable<ChannelDetail['rules']>;
};

export function ChannelRulesSection({ rules }: ChannelRulesSectionProps) {
  const { t } = useTranslation();

  const hasContent = rules.maxPostChars != null || (rules.prohibitedTopics && rules.prohibitedTopics.length > 0);
  const hasMedia = rules.mediaAllowed != null || rules.mediaTypes != null || rules.maxMediaCount != null;
  const hasLinksButtons = rules.linksAllowed != null || rules.mentionsAllowed != null || rules.maxButtons != null;
  const hasFormatting = rules.formattingAllowed != null;

  const groups: { header: string; items: React.ReactNode[] }[] = [];

  if (hasContent) {
    const items: React.ReactNode[] = [];
    if (rules.maxPostChars != null) {
      items.push(
        <GroupItem
          key="chars"
          before={<RuleIndicator allowed />}
          text={t('catalog.channel.rulesMaxChars', { count: rules.maxPostChars })}
        />,
      );
    }
    if (rules.prohibitedTopics && rules.prohibitedTopics.length > 0) {
      items.push(
        <GroupItem
          key="prohibited"
          before={<RuleIndicator allowed={false} />}
          main={
            <div className="flex flex-col gap-1.5">
              <Text type="caption1" color="secondary">
                {t('catalog.channel.rulesProhibited')}:
              </Text>
              <div className="flex flex-wrap gap-1">
                {rules.prohibitedTopics.map((topic) => (
                  <span
                    key={topic}
                    className="px-2 py-0.5 rounded-md text-xs font-medium bg-soft-destructive text-state-destructive"
                  >
                    {topic}
                  </span>
                ))}
              </div>
            </div>
          }
        />,
      );
    }
    groups.push({ header: t('catalog.channel.rulesPostSize'), items });
  }

  if (hasMedia) {
    const items: React.ReactNode[] = [];
    if (rules.mediaAllowed != null) {
      items.push(
        <GroupItem
          key="media"
          before={<RuleIndicator allowed={rules.mediaAllowed} />}
          text={rules.mediaAllowed ? t('catalog.channel.rulesMediaAllowed') : t('catalog.channel.rulesMediaNotAllowed')}
        />,
      );
    }
    if (rules.mediaTypes && rules.mediaTypes.length > 0) {
      const types = rules.mediaTypes.map((mt) => t(`catalog.channel.mediaType.${mt}`, { defaultValue: mt })).join(', ');
      items.push(
        <GroupItem
          key="types"
          before={<RuleIndicator allowed />}
          text={t('catalog.channel.rulesMediaTypes', { types })}
        />,
      );
    }
    if (rules.maxMediaCount != null) {
      items.push(
        <GroupItem
          key="count"
          before={<RuleIndicator allowed />}
          text={t('catalog.channel.rulesMaxMedia', { count: rules.maxMediaCount })}
        />,
      );
    }
    groups.push({ header: t('catalog.channel.rulesMedia'), items });
  }

  if (hasLinksButtons) {
    const items: React.ReactNode[] = [];
    if (rules.linksAllowed != null) {
      items.push(
        <GroupItem
          key="links"
          before={<RuleIndicator allowed={rules.linksAllowed} />}
          text={rules.linksAllowed ? t('catalog.channel.rulesLinksAllowed') : t('catalog.channel.rulesLinksNotAllowed')}
        />,
      );
    }
    if (rules.mentionsAllowed != null) {
      items.push(
        <GroupItem
          key="mentions"
          before={<RuleIndicator allowed={rules.mentionsAllowed} />}
          text={
            rules.mentionsAllowed
              ? t('catalog.channel.rulesMentionsAllowed')
              : t('catalog.channel.rulesMentionsNotAllowed')
          }
        />,
      );
    }
    if (rules.maxButtons != null) {
      items.push(
        <GroupItem
          key="buttons"
          before={<RuleIndicator allowed />}
          text={`${t('catalog.channel.rulesMaxButtons')}: ${t('catalog.channel.rulesButtonsCount', { count: rules.maxButtons })}`}
        />,
      );
    }
    groups.push({ header: t('catalog.channel.rulesLinks'), items });
  }

  if (hasFormatting) {
    groups.push({
      header: t('catalog.channel.rulesFormatting'),
      items: [
        <GroupItem
          key="fmt"
          before={<RuleIndicator allowed={rules.formattingAllowed as boolean} />}
          text={
            rules.formattingAllowed
              ? t('catalog.channel.rulesFormattingAllowed')
              : t('catalog.channel.rulesFormattingNotAllowed')
          }
        />,
      ],
    });
  }

  if (groups.length === 0) {
    return (
      <Group>
        <GroupItem
          text={
            <Text type="caption1" color="tertiary">
              {t('catalog.channel.noRules')}
            </Text>
          }
        />
      </Group>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {groups.map((group) => (
        <Group key={group.header} header={group.header}>
          {group.items}
        </Group>
      ))}
    </div>
  );
}
