public interface {{ obj.name()|class_name_kt }}Interface {
    {% for meth in obj.methods() -%}
    fun {{ meth.name()|fn_name_kt }}({% call kt::arg_list_decl(meth) %})
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) %}: {{ return_type|type_kt -}}
    {%- else -%}
    {%- endmatch %}
    {% endfor %}
}

class {{ obj.name()|class_name_kt }}(
    handle: Long
) : FFIObject(AtomicLong(handle)), {{ obj.name()|class_name_kt }}Interface {

    {%- match obj.primary_constructor() %}
    {%- when Some with (cons) %}
    constructor({% call kt::arg_list_decl(cons) -%}) :
        this({% call kt::to_ffi_call(cons) %})
    {%- when None %}
    {%- endmatch %}

    /**
     * Disconnect the object from the underlying Rust object.
     * 
     * It can be called more than once, but once called, interacting with the object 
     * causes an `IllegalStateException`.
     * 
     * Clients **must** call this method once done with the object, or cause a memory leak.
     */
    override fun destroy() {
        try {
            callWithHandle {
                super.destroy() // poison the handle so no-one else can use it before we tell rust.
                rustCall(InternalError.ByReference()) { err ->
                    _UniFFILib.INSTANCE.{{ obj.ffi_object_free().name() }}(it, err)
                }
            }
        } catch (e: IllegalStateException) {
            // The user called this more than once. Better than less than once.
        }
    }

    internal fun lower(): Long {
        callWithHandle { ptr -> return ptr }
    }

    internal fun write(buf: RustBufferBuilder) {
        buf.putLong(this.lower())
    }

    {% for meth in obj.methods() -%}
    {%- match meth.return_type() -%}

    {%- when Some with (return_type) -%}
    override fun {{ meth.name()|fn_name_kt }}({% call kt::arg_list_protocol(meth) %}): {{ return_type|type_kt }} =
        callWithHandle {
            {%- call kt::to_ffi_call_with_prefix("it", meth) %}
        }.let {
            {{ "it"|lift_kt(return_type) }}
        }

    {%- when None -%}
    override fun {{ meth.name()|fn_name_kt }}({% call kt::arg_list_protocol(meth) %}) =
        callWithHandle {
            {%- call kt::to_ffi_call_with_prefix("it", meth) %}
        }
    {% endmatch %}
    {% endfor %}

    companion object {
        internal fun lift(ptr: Long): {{ obj.name()|class_name_kt }} {
            return {{ obj.name()|class_name_kt }}(ptr)
            // TODO: identity map to smoosh clones together
        }

        internal fun read(buf: ByteBuffer): {{ obj.name()|class_name_kt }} {
            return {{ obj.name()|class_name_kt }}(buf.getLong())
        }

        {% for cons in obj.alternate_constructors() -%}
        fun {{ cons.name()|fn_name_kt }}({% call kt::arg_list_decl(cons) %}): {{ obj.name()|class_name_kt }} =
            {{ obj.name()|class_name_kt }}({% call kt::to_ffi_call(cons) %})
        {% endfor %}
    }
}
