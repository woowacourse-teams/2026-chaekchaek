import generateApi from './generateApi';

import { endPoint } from './endPoint';

const name = 'books';
const endpoint = endPoint['/api/v1/books'] || {};

generateApi(name, endpoint);
