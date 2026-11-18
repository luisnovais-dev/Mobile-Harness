const mod = await import('/usr/local/lib/dsh/node_modules/@deepseek-ai/dsh-attachment-local/lib/index.js');
const fs = await import('node:fs/promises');
const data = new Uint8Array(await fs.readFile('/workspace/curious-ramanujan/attachments/cc4a345f-feef-4e38-99ad-fccf3732c234/photo-20261116-101145.jpg'));
console.log('foto lida:', data.byteLength, 'bytes');
const root = '/root/.dsh/attachments/v1';
const limits = {
  maxImageBytes: 20*1024*1024, maxImagesPerMessage: 20, maxMessageImageBytes: 200*1024*1024,
  maxImagePixels: 64e6, maxImageDimension: 8192,
  mediaTypes: ['image/png','image/jpeg','image/webp','image/gif']
};
const policy = { maxPixels: 2048*2048, maxDimension: 8192, maxBytes: 4*1024*1024 };
const ref = await mod.saveImageFile(root, { data, mediaType: 'image/jpeg', name: 'photo-20261116-101145.jpg' }, limits, policy);
console.log('REF salva:', JSON.stringify(ref, null, 2));
const back = await mod.readImageFile(root, ref);
console.log('readback verificado:', back.data.byteLength, 'bytes');
