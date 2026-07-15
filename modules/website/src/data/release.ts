import releaseData from './latest-release.json';

export interface ReleaseAsset {
  name: string;
  url: string;
}

export interface ReleaseInfo {
  version: string;
  tag: string;
  assets: ReleaseAsset[];
  published_at: string;
}

const data = releaseData as ReleaseInfo;

export const latestRelease: ReleaseInfo | null =
  data.version ? data : null;

export const currentVersion: string = latestRelease?.version ?? 'dev';

export function downloadUrl(substring: string): string {
  const asset = latestRelease?.assets.find((a) =>
    a.name.toLowerCase().includes(substring.toLowerCase())
  );
  return asset?.url ?? 'https://github.com/jvmguard/jvmguard/releases/latest';
}

export function hasAsset(substring: string): boolean {
  return (
    latestRelease?.assets.some((a) =>
      a.name.toLowerCase().includes(substring.toLowerCase())
    ) ?? false
  );
}
