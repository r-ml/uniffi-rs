# Structs/Dictionaries

Dictionaries can be compared to POJOs in the Java world: just a data structure holding some data.

A Rust struct like this:

```rust
struct TodoEntry {
    done: bool,
    due_date: u64,
    text: String,
}
```

Can be exposed via UniFFI using UDL like this:

```idl
dictionary TodoEntry {
    boolean done;
    u64 due_date;
    string text;
};
```

The fields in a dictionary can be of almost any type, including objects or other dictionaries.
The current limitations are:

* They cannot recursively contain another intance of the *same* dictionary.
* They cannot contain references to callback interfaces.

## Fields holding Object References

If a dictionary contains a field whose type is an [interface](./interfaces.md), then that
field will hold a *reference* to an underlying instance of a Rust struct. The Rust code for
working with such fields must store them as an `Arc` in order to help properly manage the
lifetime of the instance. So if the UDL interface looked like this:

```idl
interface User {
    // Some sort of "user" object that can own todo items
};

dictionary TodoEntry {
    User owner;
    string text;
}
```

Then the corresponding Rust code would need to look like this:

```rust
struct TodoEntry {
    owner: std::sync::Arc<User>,
    text: String,
}
```

You can read more about managing object references in the section on [interfaces](./interfaces.md).