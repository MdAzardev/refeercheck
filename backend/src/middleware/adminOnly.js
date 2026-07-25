const { error } = require('../utils/response');

module.exports = (req, res, next) => {
  if (!req.user?.isAdmin) return error(res, 'Admin access required', 403);
  next();
};
