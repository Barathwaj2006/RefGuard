const fs = require('fs');
const path = require('path');

class SchemaValidator {
  constructor(schemasDir) {
    this.schemasDir = schemasDir || path.resolve(__dirname, '../../contracts/schemas');
    this.schemas = {};
    this.loadSchemas();
  }

  loadSchemas() {
    if (!fs.existsSync(this.schemasDir)) return;
    const files = fs.readdirSync(this.schemasDir).filter(f => f.endsWith('.json'));
    for (const f of files) {
      try {
        const raw = fs.readFileSync(path.join(this.schemasDir, f), 'utf8').replace(/^\uFEFF/, '');
        const content = JSON.parse(raw);
        this.schemas[f] = content;
        if (content.title) {
          this.schemas[content.title] = content;
        }
      } catch (err) {
        console.error('Error loading schema ' + f, err.message);
      }
    }
  }

  validate(schemaName, data) {
    const schema = this.schemas[schemaName] || this.schemas[schemaName + '.json'];
    if (!schema) {
      return { valid: true, warnings: ['Schema ' + schemaName + ' not loaded for strict validation'] };
    }
    const errors = [];
    this._validateObject(schema, data, '#', errors);
    return {
      valid: errors.length === 0,
      errors
    };
  }

  _validateObject(schema, data, currentPath, errors) {
    if (data === null || data === undefined) {
      errors.push(currentPath + ': Expected value, found ' + data);
      return;
    }

    if (schema.type === 'object') {
      if (typeof data !== 'object' || Array.isArray(data)) {
        errors.push(currentPath + ': Expected object, found ' + (Array.isArray(data) ? 'array' : typeof data));
        return;
      }

      if (schema.required && Array.isArray(schema.required)) {
        for (const req of schema.required) {
          if (!(req in data) || data[req] === undefined) {
            errors.push(currentPath + '.' + req + ': Required property missing');
          }
        }
      }

      if (schema.additionalProperties === false && schema.properties) {
        const allowed = new Set(Object.keys(schema.properties));
        for (const key of Object.keys(data)) {
          if (!allowed.has(key)) {
            errors.push(currentPath + '.' + key + ': Additional property not allowed');
          }
        }
      }

      if (schema.properties) {
        for (const [key, propSchema] of Object.entries(schema.properties)) {
          if (key in data && data[key] !== undefined) {
            this._validateProperty(propSchema, data[key], currentPath + '.' + key, errors);
          }
        }
      }
    } else if (schema.type === 'array') {
      if (!Array.isArray(data)) {
        errors.push(currentPath + ': Expected array, found ' + typeof data);
        return;
      }
      if (schema.items) {
        data.forEach((item, idx) => {
          this._validateProperty(schema.items, item, currentPath + '[' + idx + ']', errors);
        });
      }
    }
  }

  _validateProperty(propSchema, val, currentPath, errors) {
    if (propSchema['$ref']) {
      const refName = propSchema['$ref'].replace('./', '').replace('schemas/', '');
      const refSchema = this.schemas[refName];
      if (refSchema) {
        this._validateObject(refSchema, val, currentPath, errors);
        return;
      }
    }

    if (propSchema.enum && !propSchema.enum.includes(val)) {
      errors.push(currentPath + ': Value "' + val + '" is not in enum [' + propSchema.enum.join(', ') + ']');
    }

    if (propSchema.type) {
      if (propSchema.type === 'string' && typeof val !== 'string') {
        errors.push(currentPath + ': Expected string, got ' + typeof val);
      } else if (propSchema.type === 'integer' && (!Number.isInteger(val))) {
        errors.push(currentPath + ': Expected integer, got ' + val);
      } else if (propSchema.type === 'number' && typeof val !== 'number') {
        errors.push(currentPath + ': Expected number, got ' + typeof val);
      } else if (propSchema.type === 'boolean' && typeof val !== 'boolean') {
        errors.push(currentPath + ': Expected boolean, got ' + typeof val);
      } else if (propSchema.type === 'object' || propSchema.type === 'array') {
        this._validateObject(propSchema, val, currentPath, errors);
      }
    }

    if (typeof val === 'number') {
      if (propSchema.minimum !== undefined && val < propSchema.minimum) {
        errors.push(currentPath + ': Value ' + val + ' < minimum ' + propSchema.minimum);
      }
      if (propSchema.maximum !== undefined && val > propSchema.maximum) {
        errors.push(currentPath + ': Value ' + val + ' > maximum ' + propSchema.maximum);
      }
    }
  }
}

module.exports = SchemaValidator;
