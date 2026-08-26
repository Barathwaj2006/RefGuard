const Ajv = require('ajv');
const addFormats = require('ajv-formats');
const fs = require('fs');
const path = require('path');

class SchemaValidator {
  constructor(schemaDir) {
    this.ajv = new Ajv({ allErrors: true });
    addFormats(this.ajv);

    const files = fs.readdirSync(schemaDir).filter(f => f.endsWith('.json'));
    for (const f of files) {
      let content = fs.readFileSync(path.join(schemaDir, f), 'utf-8');
      if (content.charCodeAt(0) === 0xFEFF) {
        content = content.slice(1);
      }
      const schema = JSON.parse(content);
      if (!schema.$id) {
        schema.$id = f;
      }
      this.ajv.addSchema(schema, f);
    }
  }

  validate(schemaName, data) {
    const validateFn = this.ajv.getSchema(schemaName);
    if (!validateFn) throw new Error('Schema not found: ' + schemaName);
    const valid = validateFn(data);
    return {
      valid,
      errors: valid ? null : validateFn.errors.map(e => `${e.instancePath}: ${e.message}`)
    };
  }
}

module.exports = SchemaValidator;
