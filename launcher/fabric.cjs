const fs = require('node:fs/promises');
const path = require('node:path');
const { createHash, randomUUID } = require('node:crypto');

const game = '26.3';
const loader = '0.20.1';
const version = `fabric-loader-${loader}-${game}`;
const api = '0.166.2+26.3';

async function download(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(90000) });
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${url}`);
  return response;
}

async function install(file, url, sha1, cached) {
  async function read(candidate) {
    if (!candidate) return null;
    const data = await fs.readFile(candidate).catch(error => {
      if (error.code === 'ENOENT') return null;
      throw error;
    });
    return data?.length && (!sha1 || createHash('sha1').update(data).digest('hex') === sha1) ? data : null;
  }
  if (await read(file)) return file;
  const bytes = await read(cached) ?? Buffer.from(await (await download(url)).arrayBuffer());
  if (sha1 && createHash('sha1').update(bytes).digest('hex') !== sha1) {
    throw new Error(`Контрольная сумма не совпала: ${path.basename(file)}`);
  }
  await fs.mkdir(path.dirname(file), { recursive: true });
  const temporary = file + '.' + randomUUID() + '.tmp';
  try {
    await fs.writeFile(temporary, bytes);
    await fs.rename(temporary, file);
  } finally {
    await fs.rm(temporary, { force: true });
  }
  return file;
}

function allowed(rules) {
  if (!rules) return true;
  let permit = false;
  const arch = { ia32: 'x86', x64: 'x86_64', arm64: 'arm64' }[process.arch];
  for (const rule of rules) {
    if (rule.features && Object.values(rule.features).some(Boolean)) continue;
    if (rule.os?.name && rule.os.name !== 'windows') continue;
    if (rule.os?.arch && rule.os.arch !== arch && rule.os.arch !== process.arch) continue;
    permit = rule.action === 'allow';
  }
  return permit;
}

function argumentsFor(items, variables) {
  return items.flatMap(item => typeof item === 'string' ? item : allowed(item.rules) ? item.value : [])
    .map(arg => arg.replace(/\$\{([^}]+)\}/g, (_, key) => {
      if (!(key in variables)) throw new Error(`Неизвестный параметр Minecraft: ${key}`);
      return variables[key];
    }));
}

async function parallel(items, action) {
  let index = 0;
  let failure;
  await Promise.all(Array.from({ length: Math.min(12, items.length) }, async () => {
    while (!failure && index < items.length) {
      const item = items[index++];
      try { await action(item); }
      catch (error) { failure = error; }
    }
  }));
  if (failure) throw failure;
}

async function listMods(launcher, mods) {
  const read = async folder => {
    const entries = await fs.readdir(folder, { withFileTypes: true }).catch(error => {
      if (error.code === 'ENOENT') return [];
      throw error;
    });
    return entries.filter(entry => entry.isFile() && /\.jar$/i.test(entry.name)).map(entry => entry.name);
  };
  const [bundled, installed] = await Promise.all([
    read(path.join(launcher, 'bundled-mods')),
    read(mods)
  ]);
  const names = new Map(bundled.map(name => [name.toLowerCase(), name]));
  for (const name of installed) {
    const key = name.toLowerCase();
    if (!names.has(key)) names.set(key, name);
  }
  return [...names.values()].sort((a, b) => a.localeCompare(b));
}

async function installBundledMods(launcher, mods) {
  await fs.mkdir(mods, { recursive: true });
  const apiJar = path.join(launcher, 'bundled-mods', 'fabric-api.jar');
  const apiDestination = path.join(mods, 'fabric-api.jar');
  await fs.copyFile(apiJar, apiDestination).catch(async error => {
    if (error.code !== 'ENOENT') throw error;
    const base = 'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/';
    const url = `${base}${encodeURIComponent(api)}/fabric-api-${encodeURIComponent(api)}.jar`;
    const hash = (await (await download(url + '.sha1')).text()).trim();
    await install(apiDestination, url, hash);
  });
  await fs.copyFile(path.join(launcher, 'bundled-mods', 'OrbitVisuals.jar'), path.join(mods, 'orbitvisuals.jar'))
    .catch(error => { if (error.code === 'ENOENT') throw new Error('В комплекте лаунчера отсутствует OrbitVisuals.jar'); throw error; });
  const bundled = await fs.readdir(path.join(launcher, 'bundled-mods'), { withFileTypes: true });
  for (const entry of bundled) {
    if (!entry.isFile() || !/\.jar$/i.test(entry.name) || /^(fabric-api|orbitvisuals)\.jar$/i.test(entry.name)) continue;
    await fs.copyFile(path.join(launcher, 'bundled-mods', entry.name), path.join(mods, entry.name));
  }
}

async function prepareFabric(launcher, cache, username, log, instance = path.join(launcher, 'instance')) {
  if (!/^[a-zA-Z0-9_]{3,16}$/.test(username)) throw new Error('Ник: от 3 до 16 букв, цифр или подчёркиваний');
  const mods = path.join(instance, 'mods');
  const assets = path.join(instance, 'assets');
  const cached = (...parts) => cache ? path.join(cache, ...parts) : undefined;
  const json = async file => JSON.parse(await fs.readFile(file, 'utf8'));

  const versionPath = ['versions', game, game + '.json'];
  let metadata;
  try {
    metadata = await json(path.join(instance, ...versionPath));
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    const manifest = await (await download('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')).json();
    const release = manifest.versions.find(entry => entry.id === game);
    if (!release) throw new Error(`Minecraft ${game} отсутствует в манифесте Mojang`);
    metadata = await json(await install(path.join(instance, ...versionPath), release.url, release.sha1, cached(...versionPath)));
  }
  const fabric = await json(await install(path.join(instance, 'versions', version + '.json'),
    `https://meta.fabricmc.net/v2/versions/loader/${game}/${loader}/profile/json`));
  if (metadata.id !== game || fabric.id !== version || fabric.inheritsFrom !== game
      || fabric.mainClass !== 'net.fabricmc.loader.impl.launch.knot.KnotClient') {
    throw new Error('Неверные метаданные Minecraft или Fabric');
  }

  log('Проверка библиотек Minecraft и Fabric…\n');
  const libraries = [...fabric.libraries, ...metadata.libraries.filter(lib => allowed(lib.rules))];
  const seen = new Set();
  const classpath = [];
  const jobs = [];
  for (const lib of libraries) {
    const [group, artifact, release, classifier] = lib.name.split(':');
    const key = [group, artifact, classifier].join(':');
    if (seen.has(key)) continue;
    seen.add(key);
    const relative = lib.downloads?.artifact.path
      ?? `${group.replaceAll('.', '/')}/${artifact}/${release}/${artifact}-${release}${classifier ? '-' + classifier : ''}.jar`;
    const parts = ['libraries', ...relative.split('/')];
    const destination = path.join(instance, ...parts);
    classpath.push(destination);
    jobs.push(() => install(destination, lib.downloads?.artifact.url ?? new URL(relative, lib.url).href,
      lib.downloads?.artifact.sha1 ?? lib.sha1, cached(...parts)));
  }
  const clientParts = ['versions', game, game + '.jar'];
  const client = path.join(instance, ...clientParts);
  classpath.push(client);
  jobs.push(() => install(client, metadata.downloads.client.url, metadata.downloads.client.sha1, cached(...clientParts)));
  await parallel(jobs, job => job());

  const assetIndex = metadata.assetIndex;
  const indexParts = ['assets', 'indexes', assetIndex.id + '.json'];
  const index = await json(await install(path.join(instance, ...indexParts), assetIndex.url, assetIndex.sha1, cached(...indexParts)));
  const objects = [...new Set(Object.values(index.objects).map(object => object.hash))];
  let count = 0;
  log(`Проверка ресурсов: ${objects.length} файлов…\n`);
  await parallel(objects, async hash => {
    const prefix = hash.slice(0, 2);
    await install(path.join(assets, 'objects', prefix, hash), `https://resources.download.minecraft.net/${prefix}/${hash}`,
      hash, cached('assets', 'objects', prefix, hash));
    if (++count % 500 === 0) log(`Ресурсы: ${count}/${objects.length}\n`);
  });

  await installBundledMods(launcher, mods);

  const uuid = createHash('md5').update('OfflinePlayer:' + username).digest();
  uuid[6] = (uuid[6] & 15) | 48;
  uuid[8] = (uuid[8] & 63) | 128;
  const natives = path.join(instance, 'natives');
  await fs.mkdir(natives, { recursive: true });
  const variables = {
    auth_player_name: username, auth_uuid: uuid.toString('hex'), auth_access_token: '0',
    clientid: '', auth_xuid: '', version_name: version, version_type: 'release',
    game_directory: instance, assets_root: assets, assets_index_name: assetIndex.id,
    natives_directory: natives, launcher_name: 'OrbitVisuals', launcher_version: '1.0.0',
    classpath: classpath.join(path.delimiter)
  };
  return {
    instance,
    args: ['-Xms512M', '-Xmx4G', ...argumentsFor([...metadata.arguments.jvm, ...fabric.arguments.jvm], variables),
      fabric.mainClass, ...argumentsFor([...metadata.arguments.game, ...fabric.arguments.game], variables)]
  };
}

module.exports = { prepareFabric, installBundledMods, listMods };
