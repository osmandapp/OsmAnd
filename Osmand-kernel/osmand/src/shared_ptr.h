// Simple shared_ptr implementation based on http://stackoverflow.com/questions/1512520/decent-shared-ptr-implementation-that-does-not-require-a-massive-library
template <typename contained>
class my_shared_ptr {
public:
   my_shared_ptr() : ptr_(NULL), ref_count_(NULL) { }

   my_shared_ptr(contained * p)
     : ptr_(p), ref_count_(p ? new int : NULL)
   { inc_ref(); }

   my_shared_ptr(const my_shared_ptr& rhs)
     : ptr_(rhs.ptr_), ref_count_(rhs.ref_count_)
   { inc_ref(); }

   ~my_shared_ptr() {
     if(ref_count_ && 0 == dec_ref()) { delete ptr_; delete ref_count_; }
   }
   contained * get() { return ptr_; }
   const contained * get() const { return ptr_; }

   void swap(my_shared_ptr& rhs) // throw()
   {
      std::swap(ptr_, rhs.ptr_);
      std::swap(ref_count_, rhs.ref_count_);
   }

   my_shared_ptr& operator=(const my_shared_ptr& rhs) {
        my_shared_ptr tmp(rhs);
        this->swap(tmp);
        return *this;
   }

   contained *  operator->() {
        return this->ptr_;
   }

   // operator->, operator*, operator void*, use_count
private:
   void inc_ref() {
      if(ref_count_) { ++(*ref_count_); }
   }

   int  dec_ref() {
      return --(*ref_count_);
   }

   contained * ptr_;
   int * ref_count_;
};
