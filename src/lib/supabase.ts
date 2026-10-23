import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const key = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY as string | undefined;
export const cloudConfigured = Boolean(url && key && !url.includes('YOUR_PROJECT') && !key.includes('replace_me'));
export const supabase = cloudConfigured ? createClient(url!, key!) : null;
