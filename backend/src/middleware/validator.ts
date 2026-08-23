import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import { Request, Response, NextFunction } from 'express';
import fs from 'fs';
import path from 'path';

const ajv = new Ajv({ allErrors: true, schemas: [] });
addFormats(ajv);

// Load all schemas
const schemaDir = path.join(__dirname, '../../../contracts/schemas');
const schemaFiles = fs.readdirSync(schemaDir).filter(file => file.endsWith('.json'));

for (const file of schemaFiles) {
    const schemaPath = path.join(schemaDir, file);
    let rawContent = fs.readFileSync(schemaPath, 'utf-8');
    // Remove BOM if present
    if (rawContent.charCodeAt(0) === 0xFEFF) {
        rawContent = rawContent.slice(1);
    }
    const schemaContent = JSON.parse(rawContent);
    // Need to assign a local $id if not present for cross-referencing to work easily
    if (!schemaContent.$id) {
        schemaContent.$id = file;
    }
    ajv.addSchema(schemaContent);
}

export const validateRequest = (schemaName: string) => {
    return (req: Request, res: Response, next: NextFunction) => {
        const validate = ajv.getSchema(schemaName);

        if (!validate) {
            return res.status(500).json({
                error_code: "INTERNAL_ERROR",
                error_message: `Schema not found: ${schemaName}`
            });
        }

        const valid = validate(req.body);

        if (!valid) {
            const errors = validate.errors?.map(e => `${e.instancePath} ${e.message}`).join(', ');
            return res.status(400).json({
                error_code: "INVALID_REQUEST",
                error_message: "Request validation failed",
                details: errors
            });
        }

        next();
    };
};

export const validateResponse = (schemaName: string) => {
    return (req: Request, res: Response, next: NextFunction) => {
        const validate = ajv.getSchema(schemaName);

        if (!validate) {
            return res.status(500).json({
                error_code: "INTERNAL_ERROR",
                error_message: `Schema not found: ${schemaName}`
            });
        }

        const originalJson = res.json;
        res.json = function (body) {
            // Bypass validation for error responses (400, 500)
            if (res.statusCode >= 400) {
               return originalJson.call(this, body);
            }

            const valid = validate(body);
            if (!valid) {
                const errors = validate.errors?.map(e => `${e.instancePath} ${e.message}`).join(', ');
                console.error('Response Validation Failed:', errors);
                this.status(500);
                return originalJson.call(this, {
                    error_code: "INTERNAL_SERVER_ERROR",
                    error_message: "Response validation failed against schema contract",
                    details: errors
                });
            }
            return originalJson.call(this, body);
        };

        next();
    };
};
