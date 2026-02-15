import { api } from '@/shared/api/client';
import { type DepositInfo, DepositInfoSchema } from '../types/ton';

export function fetchDealDepositInfo(dealId: string): Promise<DepositInfo> {
  return api.get(`/deals/${dealId}/deposit`, { schema: DepositInfoSchema });
}
