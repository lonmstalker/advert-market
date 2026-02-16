import { mockProfile } from '@/test/mocks/data';
import { preserveLanguageOnSettingsUpdate } from './profile-settings';

describe('preserveLanguageOnSettingsUpdate', () => {
  it('keeps server profile when language is unchanged', () => {
    const result = preserveLanguageOnSettingsUpdate(mockProfile, 'ru');

    expect(result).toEqual(mockProfile);
  });

  it('preserves current language when settings response returns stale language', () => {
    const staleProfile = { ...mockProfile, languageCode: 'en' };

    const result = preserveLanguageOnSettingsUpdate(staleProfile, 'ru');

    expect(result).toEqual({ ...staleProfile, languageCode: 'ru' });
  });
});
