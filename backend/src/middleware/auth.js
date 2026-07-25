const { verifyAccessToken } = require('../utils/jwt');
const { error } = require('../utils/response');

module.exports = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) return error(res, 'Unauthorized', 401);
  try {
    const token = authHeader.split(' ')[1];
    req.user = verifyAccessToken(token);
    next();
  } catch {
    return error(res, 'Token invalid or expired', 401);
  }
};
