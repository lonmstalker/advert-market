import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  DefaultPostIcon,
  ForwardIcon,
  GiveawayIcon,
  IntegrationIcon,
  MentionIcon,
  NativePostIcon,
  PinnedIcon,
  PollIcon,
  PostTypeIcon,
  RepostIcon,
  ReviewIcon,
  StoryIcon,
} from './post-type-icons';

describe('PostTypeIcon individual icons', () => {
  it('renders NativePostIcon as an SVG', () => {
    const { container } = render(<NativePostIcon data-testid="icon" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });

  it('renders StoryIcon as an SVG', () => {
    const { container } = render(<StoryIcon />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('viewBox', '0 0 24 24');
  });

  it('renders RepostIcon as an SVG', () => {
    const { container } = render(<RepostIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders ForwardIcon as an SVG', () => {
    const { container } = render(<ForwardIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders IntegrationIcon as an SVG', () => {
    const { container } = render(<IntegrationIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders ReviewIcon as an SVG', () => {
    const { container } = render(<ReviewIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders MentionIcon as an SVG', () => {
    const { container } = render(<MentionIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders GiveawayIcon as an SVG', () => {
    const { container } = render(<GiveawayIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders PinnedIcon as an SVG', () => {
    const { container } = render(<PinnedIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders PollIcon as an SVG', () => {
    const { container } = render(<PollIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders DefaultPostIcon as an SVG', () => {
    const { container } = render(<DefaultPostIcon />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('passes custom props through to SVG element', () => {
    const { container } = render(<NativePostIcon width={32} height={32} className="custom" />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '32');
    expect(svg).toHaveAttribute('height', '32');
    expect(svg).toHaveClass('custom');
  });
});

describe('PostTypeIcon composite component', () => {
  it('renders NativePostIcon for postType=NATIVE', () => {
    const { container } = render(<PostTypeIcon postType="NATIVE" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg?.querySelector('polyline')).toBeInTheDocument();
  });

  it('renders StoryIcon for postType=STORY', () => {
    const { container } = render(<PostTypeIcon postType="STORY" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg?.querySelector('rect')).toBeInTheDocument();
  });

  it('renders RepostIcon for postType=REPOST', () => {
    const { container } = render(<PostTypeIcon postType="REPOST" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders ForwardIcon for postType=FORWARD', () => {
    const { container } = render(<PostTypeIcon postType="FORWARD" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders IntegrationIcon for postType=INTEGRATION', () => {
    const { container } = render(<PostTypeIcon postType="INTEGRATION" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders ReviewIcon for postType=REVIEW', () => {
    const { container } = render(<PostTypeIcon postType="REVIEW" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders MentionIcon for postType=MENTION', () => {
    const { container } = render(<PostTypeIcon postType="MENTION" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders GiveawayIcon for postType=GIVEAWAY', () => {
    const { container } = render(<PostTypeIcon postType="GIVEAWAY" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders PinnedIcon for postType=PINNED', () => {
    const { container } = render(<PostTypeIcon postType="PINNED" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders PollIcon for postType=POLL', () => {
    const { container } = render(<PostTypeIcon postType="POLL" />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders DefaultPostIcon for unknown postType', () => {
    const { container } = render(<PostTypeIcon postType="UNKNOWN_TYPE" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg?.querySelector('polygon')).toBeInTheDocument();
  });

  it('handles lowercase postType via toUpperCase()', () => {
    const { container } = render(<PostTypeIcon postType="native" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg?.querySelector('polyline')).toBeInTheDocument();
  });

  it('passes extra SVG props to the resolved icon', () => {
    const { container } = render(<PostTypeIcon postType="STORY" width={40} height={40} />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '40');
    expect(svg).toHaveAttribute('height', '40');
  });
});
