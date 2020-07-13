public struct {{ rec.name() }}: Lowerable, Liftable, Serializable {
    {%- for field in rec.fields() %}
    let {{ field.name() }}: {{ field.type_()|decl_swift }}
    {%- endfor %}

    // Default memberwise initializers are never public by default, so we
    // declare one manually.
    public init(
        {%- for field in rec.fields() %}
        {{ field.name() }}: {{ field.type_()|decl_swift }}{% if loop.last %}{% else %},{% endif %}
        {%- endfor %}
    ) {
        {%- for field in rec.fields() %}
        self.{{ field.name() }} = {{ field.name() }}
        {%- endfor %}
    }

    static func lift(from buf: Reader) throws -> {{ rec.name() }} {
        return try {{ rec.name() }}(
            {%- for field in rec.fields() %}
            {{ field.name() }}: {{ "buf"|lift_from_swift(field.type_()) }}{% if loop.last %}{% else %},{% endif %}
            {%- endfor %}
        )
    }

    func lower(into buf: Writer) {
        {%- for field in rec.fields() %}
        {{ field.name() }}.lower(into: buf)
        {%- endfor %}
    }
}