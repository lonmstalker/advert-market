import type { Meta, StoryObj } from '@storybook/react-vite';
import { Image } from '@telegram-tools/ui-kit';
import { motion } from 'motion/react';
import { fadeIn, scaleIn } from '../animations';

const meta: Meta<typeof Image> = {
  title: 'UI Kit/Image',
  component: Image,
  argTypes: {
    objectFit: { control: 'select', options: ['cover', 'contain', 'fill', 'none'] },
    borderRadius: { control: 'text' },
    width: { control: 'text' },
    height: { control: 'text' },
  },
};

export default meta;
type Story = StoryObj<typeof Image>;

const PLACEHOLDER = 'https://placehold.co/300x200/4A90D9/white?text=Channel+Cover';
const AVATAR = 'https://placehold.co/80x80/7B61FF/white?text=TG';

export const Default: Story = {
  args: { src: PLACEHOLDER, width: '300px', height: '200px', borderRadius: '12px' },
};

export const Avatar: Story = {
  args: { src: AVATAR, width: '80px', height: '80px', borderRadius: '50%', objectFit: 'cover' },
};

export const WithFallback: Story = {
  args: {
    src: 'https://broken-url.example/image.png',
    fallback: PLACEHOLDER,
    width: '300px',
    height: '200px',
    borderRadius: '12px',
  },
};

export const AspectRatio: Story = {
  args: { src: PLACEHOLDER, width: '100%', aspectRatio: '16/9', borderRadius: '12px', objectFit: 'cover' },
};

export const AnimatedFadeIn: Story = {
  render: (args) => (
    <motion.div {...fadeIn}>
      <Image {...args} />
    </motion.div>
  ),
  args: { src: PLACEHOLDER, width: '300px', height: '200px', borderRadius: '12px' },
};

export const AnimatedScale: Story = {
  render: (args) => (
    <motion.div {...scaleIn}>
      <Image {...args} />
    </motion.div>
  ),
  args: { src: AVATAR, width: '100px', height: '100px', borderRadius: '50%' },
};

export const Gallery: Story = {
  render: () => (
    <div className="grid grid-cols-3 gap-2">
      {[1, 2, 3, 4, 5, 6].map((i) => (
        <motion.div key={i} {...fadeIn} transition={{ delay: i * 0.1, duration: 0.3 }}>
          <Image
            src={`https://placehold.co/120x120/4A90D9/white?text=${i}`}
            width="120px"
            height="120px"
            borderRadius="8px"
            objectFit="cover"
          />
        </motion.div>
      ))}
    </div>
  ),
};
