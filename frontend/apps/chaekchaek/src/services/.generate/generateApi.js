import path from 'node:path';
import fs from 'node:fs';

const generateType = 'apis';

const generate = (name, endPoint) => {
  if (!name) throw new Error('You must include a component name');

  const dir = path.resolve(`./src/services/${generateType}/`, `./${name}/`);

  if (fs.existsSync(dir)) throw new Error('A component with that name already exists');

  fs.mkdirSync(dir);

  function writeFileErrorHandler(err) {
    if (err) throw err;
  }

  const componentDir = path.resolve(`./src/services/.generate/${generateType}/`);

  fs.readdir(componentDir, (err, files) => {
    files?.forEach(async (file) => {
      let filename = file;
      filename = filename.replace(/\.js$/, '.ts');
      const { component } = await import(path.resolve(componentDir, file));

      const content = component(name, endPoint);

      fs.writeFile(`${dir}/${filename}`, content, writeFileErrorHandler);
    });
  });
};

export default generate;
